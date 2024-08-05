package com.cst438.controller;


import com.cst438.domain.*;
import com.cst438.dto.EnrollmentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class EnrollmentController {

    // instructor downloads student enrollments for a section, ordered by student name
    // user must be instructor for the section
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TermRepository termRepository;

    @GetMapping("/sections/{sectionNo}/enrollments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<EnrollmentDTO> getEnrollments(
            @PathVariable("sectionNo") int sectionNo,
            Principal principal) {

        // TODO
		//  hint: use enrollment repository findEnrollmentsBySectionNoOrderByStudentName method
        //  remove the following line when done

        Section sec = sectionRepository.findById(sectionNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found."));

        // Perform the security check
        if (sec.getInstructorEmail() == null || !sec.getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized User");
        }

        try {
            List<Enrollment> enrollments = enrollmentRepository.findEnrollmentsBySectionNoOrderByStudentName(sectionNo);

            return enrollments.stream()
                    .map(enrollment -> {
                        User user = enrollment.getUser();
                        Section section = enrollment.getSection();
                        Course course = section.getCourse();
                        Term term = section.getTerm();

                        return new EnrollmentDTO(
                                enrollment.getEnrollmentId(),
                                enrollment.getGrade(),
                                user.getId(),
                                user.getName(),
                                user.getEmail(),
                                course.getCourseId(),
                                course.getTitle(),
                                section.getSecId(),
                                section.getSectionNo(),
                                section.getBuilding(),
                                section.getRoom(),
                                section.getTimes(),
                                course.getCredits(),
                                term.getYear(),
                                term.getSemester()
                        );
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found", e);
        }

    }

    // instructor uploads enrollments with the final grades for the section
    // user must be instructor for the section
    @PutMapping("/enrollments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public void updateEnrollmentGrade(@RequestBody List<EnrollmentDTO> dlist, Principal principal) {

        // TODO

        // For each EnrollmentDTO in the list
        //  find the Enrollment entity using enrollmentId
        //  update the grade and save back to database

        try {
            for (EnrollmentDTO dto : dlist) {
                Enrollment enrollment = enrollmentRepository.findById(dto.enrollmentId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));
                enrollment.setGrade(dto.grade());
                enrollmentRepository.save(enrollment);

                if(enrollment.getSection().getInstructorEmail() == null || !enrollment.getSection().getInstructorEmail().equals(principal.getName())) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized User");
                }
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error updating grades", e);
        }
    }

    @PostMapping("/enrollments")
    public ResponseEntity<?> enrollStudent(@RequestBody EnrollmentDTO enrollmentDTO) {
        try {
            Section section = sectionRepository.findById(enrollmentDTO.sectionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));

            User student = userRepository.findById(enrollmentDTO.studentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

            Enrollment enrollment = new Enrollment();
            enrollment.setSection(section);
            enrollment.setUser(student);
            enrollmentRepository.save(enrollment);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error enrolling student", e);
        }
    }


}
