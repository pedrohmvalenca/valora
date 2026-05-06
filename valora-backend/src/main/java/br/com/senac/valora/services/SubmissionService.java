package br.com.senac.valora.services;

import br.com.senac.valora.dtos.ApproveSubmissionRequest;
import br.com.senac.valora.dtos.BalanceDto;
import br.com.senac.valora.dtos.HistoryItemDto;
import br.com.senac.valora.dtos.RejectSubmissionRequest;
import br.com.senac.valora.dtos.SubmissionDetailDto;
import br.com.senac.valora.dtos.SubmissionListItemDto;
import br.com.senac.valora.entities.Category;
import br.com.senac.valora.entities.CategoryCourse;
import br.com.senac.valora.entities.Course;
import br.com.senac.valora.entities.Student;
import br.com.senac.valora.entities.Submission;
import br.com.senac.valora.entities.SubmissionStatus;
import br.com.senac.valora.entities.User;
import br.com.senac.valora.entities.UserProfile;
import br.com.senac.valora.exceptions.BusinessRuleException;
import br.com.senac.valora.exceptions.ErrorCode;
import br.com.senac.valora.repositories.CategoryCourseRepository;
import br.com.senac.valora.repositories.CategoryRepository;
import br.com.senac.valora.repositories.CoordinatorCourseRepository;
import br.com.senac.valora.repositories.CourseRepository;
import br.com.senac.valora.repositories.StudentRepository;
import br.com.senac.valora.repositories.SubmissionRepository;
import br.com.senac.valora.repositories.UserRepository;
import br.com.senac.valora.security.JwtAuthentication;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmissionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);

    private final SubmissionRepository submissionRepo;
    private final StudentRepository studentRepo;
    private final CourseRepository courseRepo;
    private final CategoryRepository categoryRepo;
    private final CategoryCourseRepository categoryCourseRepo;
    private final CoordinatorCourseRepository coordCourseRepo;
    private final UserRepository userRepo;
    private final AuditService auditService;

    public SubmissionService(
            SubmissionRepository submissionRepo,
            StudentRepository studentRepo,
            CourseRepository courseRepo,
            CategoryRepository categoryRepo,
            CategoryCourseRepository categoryCourseRepo,
            CoordinatorCourseRepository coordCourseRepo,
            UserRepository userRepo,
            AuditService auditService) {
        this.submissionRepo = submissionRepo;
        this.studentRepo = studentRepo;
        this.courseRepo = courseRepo;
        this.categoryRepo = categoryRepo;
        this.categoryCourseRepo = categoryCourseRepo;
        this.coordCourseRepo = coordCourseRepo;
        this.userRepo = userRepo;
        this.auditService = auditService;
    }


    @Transactional(readOnly = true)
    public Page<SubmissionListItemDto> list(
            JwtAuthentication auth, SubmissionStatus status, UUID courseId, Pageable pageable) {

        Page<Submission> page = fetchScoped(auth, status, courseId, pageable);
        return page.map(this::toListItem);
    }

    private Page<Submission> fetchScoped(
            JwtAuthentication auth, SubmissionStatus status, UUID courseId, Pageable pageable) {

        boolean isAdmin = auth.profile() == UserProfile.ADMINISTRATOR;
        if (isAdmin) {
            if (courseId != null && status != null) {
                return submissionRepo.findByCourseIdAndStatusOrderByCreatedAtDesc(courseId, status, pageable);
            }
            if (courseId != null) {
                return submissionRepo.findByCourseIdOrderByCreatedAtDesc(courseId, pageable);
            }
            if (status != null) {
                return submissionRepo.findByStatusOrderByCreatedAtDesc(status, pageable);
            }
            return submissionRepo.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<UUID> linkedCourses = coordCourseRepo.findCourseIdsByCoordinatorId(auth.userId());
        if (linkedCourses.isEmpty()) {
            return Page.empty(pageable);
        }
        if (courseId != null && !linkedCourses.contains(courseId)) {
            return Page.empty(pageable);
        }
        List<UUID> effectiveCourses = courseId != null ? List.of(courseId) : linkedCourses;
        if (status != null) {
            return submissionRepo.findByCourseIdInAndStatusOrderByCreatedAtDesc(effectiveCourses, status, pageable);
        }
        return submissionRepo.findByCourseIdInOrderByCreatedAtDesc(effectiveCourses, pageable);
    }


    @Transactional(readOnly = true)
    public SubmissionDetailDto getDetail(UUID submissionId, JwtAuthentication auth) {
        Submission s = submissionRepo.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submissão não encontrada: id=" + submissionId));

        verifyAccess(s, auth);

        Student student = studentRepo.findById(s.getStudentId())
                .orElseThrow(() -> new EntityNotFoundException("Aluno não encontrado"));
        Course course = courseRepo.findById(s.getCourseId())
                .orElseThrow(() -> new EntityNotFoundException("Curso não encontrado"));
        Category category = categoryRepo.findById(s.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada"));

        BalanceDto balance = computeBalance(s.getStudentId(), s.getCourseId(), s.getCategoryId());
        List<HistoryItemDto> history = buildHistory(s);

        return new SubmissionDetailDto(
                s.getId(), s.getStudentId(), student.getName(), student.getRegistrationCode(),
                s.getCourseId(), course.getName(),
                s.getCategoryId(), category.getName(), category.getGroupType(),
                s.getDescription(), s.getRequestedHours(), s.getRecognizedHours(), s.getProofPath(),
                s.getStatus(), s.getDecidedBy(), s.getDecidedAt(), s.getRejectionReason(),
                s.getCreatedAt(), balance, history);
    }

    private List<HistoryItemDto> buildHistory(Submission current) {
        List<Submission> previous = submissionRepo
                .findTop10ByStudentIdAndCourseIdAndCategoryIdAndIdNotOrderByCreatedAtDesc(
                        current.getStudentId(), current.getCourseId(),
                        current.getCategoryId(), current.getId());
        return previous.stream()
                .map(p -> new HistoryItemDto(p.getId(), p.getDescription(), p.getRequestedHours(),
                        p.getRecognizedHours(), p.getStatus(), p.getCreatedAt(), p.getDecidedAt()))
                .toList();
    }

    private BalanceDto computeBalance(UUID studentId, UUID courseId, UUID categoryId) {
        int max = categoryCourseRepo.findByCategoryIdAndCourseId(categoryId, courseId)
                .map(CategoryCourse::getMaxHours)
                .orElse(0);
        int accumulated = submissionRepo.sumApprovedHours(studentId, courseId, categoryId);
        int remaining = Math.max(0, max - accumulated);
        return new BalanceDto(max, accumulated, remaining);
    }

    @Transactional
    public void approve(UUID submissionId, ApproveSubmissionRequest req, JwtAuthentication auth) {
        Submission s = loadAndValidatePending(submissionId, auth);

        int recognized = (req != null && req.recognizedHours() != null)
                ? req.recognizedHours() : s.getRequestedHours();

        if (recognized > s.getRequestedHours()) {
            throw new BusinessRuleException(
                    ErrorCode.CATEGORY_HOURS_LIMIT_EXCEEDED,
                    String.format("Horas reconhecidas (%dh) excedem solicitadas (%dh).",
                            recognized, s.getRequestedHours()));
        }

        BalanceDto balance = computeBalance(s.getStudentId(), s.getCourseId(), s.getCategoryId());
        if (recognized > balance.remainingHours()) {
            throw new BusinessRuleException(
                    ErrorCode.CATEGORY_HOURS_LIMIT_EXCEEDED,
                    String.format("Aprovação excede saldo (%dh restantes na categoria/curso) — tentou aprovar %dh. "
                                    + "Use 'Ajustar p/ %dh' para aprovar parcialmente.",
                            balance.remainingHours(), recognized, balance.remainingHours()));
        }

        s.setStatus(SubmissionStatus.APPROVED);
        s.setRecognizedHours(recognized);
        s.setDecidedBy(auth.userId());
        s.setDecidedAt(Instant.now());
        submissionRepo.save(s);

        log.info("Submissão aprovada: id={} reconhecidas={}h por userId={}",
                submissionId, recognized, auth.userId());
        auditService.recordSubmissionDecision("APPROVE_SUBMISSION", auth.userId(), submissionId, s.getCourseId());
    }

    @Transactional
    public void revertDecision(UUID submissionId, JwtAuthentication auth) {
        // Profile check removido — @PreAuthorize no controller já garante Admin.
        Submission s = submissionRepo.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submissão não encontrada: id=" + submissionId));

        if (s.getStatus() == SubmissionStatus.PENDING) {
            throw new BusinessRuleException(ErrorCode.SUBMISSION_STATUS_IMMUTABLE,
                    "Submissão já está PENDING — nada a reverter");
        }

        s.setStatus(SubmissionStatus.PENDING);
        s.setRecognizedHours(null);
        s.setDecidedBy(null);
        s.setDecidedAt(null);
        s.setRejectionReason(null);
        submissionRepo.save(s);

        log.warn("DECISÃO REVERTIDA (modo demo): submissionId={} por adminId={}", submissionId, auth.userId());
        auditService.recordSubmissionDecision("REVERT_SUBMISSION_DECISION", auth.userId(),
                submissionId, s.getCourseId());
    }

    @Transactional
    public void reject(UUID submissionId, RejectSubmissionRequest req, JwtAuthentication auth) {
        Submission s = loadAndValidatePending(submissionId, auth);

        s.setStatus(SubmissionStatus.REJECTED);
        s.setRejectionReason(req.reason());
        s.setDecidedBy(auth.userId());
        s.setDecidedAt(Instant.now());
        submissionRepo.save(s);

        log.info("Submissão reprovada: id={} por userId={}", submissionId, auth.userId());
        auditService.recordSubmissionDecision("REJECT_SUBMISSION", auth.userId(), submissionId, s.getCourseId());
    }


    private Submission loadAndValidatePending(UUID submissionId, JwtAuthentication auth) {
        Submission s = submissionRepo.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submissão não encontrada: id=" + submissionId));

        verifyAccess(s, auth);

        if (s.getStatus() != SubmissionStatus.PENDING) {
            throw new BusinessRuleException(
                    ErrorCode.SUBMISSION_STATUS_IMMUTABLE,
                    "Apenas submissões com status 'Pendente' podem ser decididas. Status atual: "
                            + s.getStatus().name());
        }
        return s;
    }

    private void verifyAccess(Submission s, JwtAuthentication auth) {
        if (auth.profile() == UserProfile.ADMINISTRATOR) {
            return; // Admin acessa tudo
        }
        if (auth.profile() == UserProfile.COORDINATOR) {
            List<UUID> linked = coordCourseRepo.findCourseIdsByCoordinatorId(auth.userId());
            if (!linked.contains(s.getCourseId())) {
                throw new EntityNotFoundException("Submissão não encontrada: id=" + s.getId());
            }
            return;
        }
        throw new EntityNotFoundException("Submissão não encontrada: id=" + s.getId());
    }

    private SubmissionListItemDto toListItem(Submission s) {
        Student student = studentRepo.findById(s.getStudentId()).orElse(null);
        Course course = courseRepo.findById(s.getCourseId()).orElse(null);
        Category category = categoryRepo.findById(s.getCategoryId()).orElse(null);
        return new SubmissionListItemDto(
                s.getId(),
                student != null ? student.getName() : "?",
                student != null ? student.getRegistrationCode() : "?",
                course != null ? course.getName() : "?",
                category != null ? category.getName() : "?",
                s.getRequestedHours(),
                s.getStatus(),
                s.getCreatedAt());
    }
}
