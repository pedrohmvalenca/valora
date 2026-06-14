package br.com.senac.valora.controllers;

import br.com.senac.valora.dtos.CreateStudentRequest;
import br.com.senac.valora.dtos.LinkStudentCoursesRequest;
import br.com.senac.valora.dtos.StudentCourseStatusDto;
import br.com.senac.valora.dtos.StudentDto;
import br.com.senac.valora.dtos.StudentSearchResultDto;
import br.com.senac.valora.dtos.UpdateStudentCourseStatusRequest;
import br.com.senac.valora.security.JwtAuthentication;
import br.com.senac.valora.services.StudentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students")
@PreAuthorize("hasAnyRole('COORDINATOR','ADMINISTRATOR')")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public ResponseEntity<List<StudentDto>> list(JwtAuthentication auth) {
        return ResponseEntity.ok(studentService.list(auth));
    }

    @PostMapping
    public ResponseEntity<StudentDto> create(@Valid @RequestBody CreateStudentRequest req,
                                             JwtAuthentication auth) {
        StudentDto created = studentService.create(req, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/search")
    public ResponseEntity<List<StudentSearchResultDto>> search(@RequestParam("q") String q) {
        return ResponseEntity.ok(studentService.search(q));
    }

    @PostMapping("/{studentId}/courses")
    public ResponseEntity<StudentDto> linkCourses(@PathVariable UUID studentId,
                                                  @Valid @RequestBody LinkStudentCoursesRequest req,
                                                  JwtAuthentication auth) {
        return ResponseEntity.ok(studentService.linkCourses(studentId, req, auth));
    }

    @GetMapping("/{studentId}/courses")
    public ResponseEntity<List<StudentCourseStatusDto>> listCoursesWithStatus(
            @PathVariable UUID studentId, JwtAuthentication auth) {
        return ResponseEntity.ok(studentService.listCoursesWithStatus(studentId, auth));
    }

    @PatchMapping("/{studentId}/courses/{courseId}/status")
    public ResponseEntity<StudentCourseStatusDto> updateCourseStatus(
            @PathVariable UUID studentId,
            @PathVariable UUID courseId,
            @Valid @RequestBody UpdateStudentCourseStatusRequest req,
            JwtAuthentication auth) {
        return ResponseEntity.ok(studentService.updateCourseStatus(studentId, courseId, req, auth));
    }
}
