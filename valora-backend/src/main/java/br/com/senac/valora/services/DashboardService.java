package br.com.senac.valora.services;

import br.com.senac.valora.dtos.DashboardCoordinatorItemDto;
import br.com.senac.valora.entities.Category;
import br.com.senac.valora.entities.UserProfile;
import br.com.senac.valora.repositories.CategoryRepository;
import br.com.senac.valora.repositories.CoordinatorCourseRepository;
import br.com.senac.valora.repositories.SubmissionRepository;
import br.com.senac.valora.security.JwtAuthentication;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final SubmissionRepository submissionRepo;
    private final CategoryRepository categoryRepo;
    private final CoordinatorCourseRepository coordCourseRepo;

    public DashboardService(SubmissionRepository submissionRepo,
                            CategoryRepository categoryRepo,
                            CoordinatorCourseRepository coordCourseRepo) {
        this.submissionRepo = submissionRepo;
        this.categoryRepo = categoryRepo;
        this.coordCourseRepo = coordCourseRepo;
    }

    @Transactional(readOnly = true)
    public List<DashboardCoordinatorItemDto> coordinatorDashboard(JwtAuthentication auth, UUID courseId) {
        Map<UUID, Integer> hoursByCategory = aggregateHoursByCategory(auth, courseId);
        List<Category> categories = categoryRepo.findAllByOrderByNameAsc();
        return categories.stream()
                .map(c -> new DashboardCoordinatorItemDto(
                        c.getName(),
                        hoursByCategory.getOrDefault(c.getId(), 0)))
                .sorted(Comparator
                        .comparingInt(DashboardCoordinatorItemDto::horasReconhecidas).reversed()
                        .thenComparing(DashboardCoordinatorItemDto::categoria, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Map<UUID, Integer> aggregateHoursByCategory(JwtAuthentication auth, UUID courseId) {
        if (auth.profile() == UserProfile.ADMINISTRATOR) {
            List<Object[]> rows = courseId != null
                    ? submissionRepo.sumApprovedHoursByCategoryForCourse(courseId)
                    : submissionRepo.sumApprovedHoursByCategoryAll();
            return toMap(rows);
        }
        if (auth.profile() == UserProfile.COORDINATOR) {
            List<UUID> linkedCourses = coordCourseRepo.findCourseIdsByCoordinatorId(auth.userId());
            if (courseId != null) {
                if (!linkedCourses.contains(courseId)) {
                    throw new AccessDeniedException("Curso fora do escopo do coordenador");
                }
                return toMap(submissionRepo.sumApprovedHoursByCategoryForCourse(courseId));
            }
            if (linkedCourses.isEmpty()) {
                return Map.of();
            }
            return toMap(submissionRepo.sumApprovedHoursByCategoryForCourses(linkedCourses));
        }
        throw new AccessDeniedException("Recurso restrito a coordenadores e administradores");
    }

    private static Map<UUID, Integer> toMap(List<Object[]> rows) {
        Map<UUID, Integer> out = new HashMap<>();
        for (Object[] row : rows) {
            UUID categoryId = (UUID) row[0];
            Number sum = (Number) row[1];
            out.put(categoryId, sum == null ? 0 : sum.intValue());
        }
        return out;
    }
}
