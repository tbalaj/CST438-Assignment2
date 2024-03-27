package com.cst438.controller;


import com.cst438.domain.*;
import com.cst438.dto.EnrollmentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000", methods = {RequestMethod.GET, RequestMethod.PUT, RequestMethod.POST})
public class EnrollmentController {

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    SectionRepository sectionRepository;

    @Autowired
    TermRepository termRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EnrollmentRepository enrollmentRepository;

    // instructor downloads student enrollments for a section, ordered by student name
    // user must be instructor for the section
    @GetMapping("/sections/{sectionNo}/enrollments")
    public List<EnrollmentDTO> getEnrollments(
            @PathVariable("sectionNo") int sectionNo ) {
        List<EnrollmentDTO> dto_list = new ArrayList<>();

        List<Enrollment> enrollments = enrollmentRepository.findEnrollmentsBySectionNoOrderByStudentName(sectionNo);

        if(enrollments == null){
            throw  new ResponseStatusException( HttpStatus.NOT_FOUND, "section number is invalid");
        }

        for (Enrollment e : enrollments){
            dto_list.add(new EnrollmentDTO(
                    e.getEnrollmentId(),
                    e.getGrade(),
                    e.getUser().getId(),
                    e.getUser().getName(),
                    e.getUser().getEmail(),
                    e.getSection().getCourse().getCourseId(),
                    e.getSection().getSecId(),
                    e.getSection().getSectionNo(),
                    e.getSection().getBuilding(),
                    e.getSection().getRoom(),
                    e.getSection().getTimes(),
                    e.getSection().getCourse().getCredits(),
                    e.getSection().getTerm().getYear(),
                    e.getSection().getTerm().getSemester()));
        }

        return dto_list;

    }

    // instructor uploads enrollments with the final grades for the section
    // user must be instructor for the section
    @PutMapping("/enrollments")
    public void updateEnrollmentGrade(@RequestBody List<EnrollmentDTO> dlist) {

        for(EnrollmentDTO e : dlist){
            Enrollment f = enrollmentRepository.findById(e.enrollmentId()).orElse(null);
            if (f==null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "enrollment not found "+e.enrollmentId());
            }
            else {
                f.setGrade(e.grade());
                enrollmentRepository.save(f);
            }
        }
    }
}