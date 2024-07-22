package com.cst438.controller;

import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.dto.EnrollmentDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.cst438.test.utils.TestUtils.asJsonString;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

@AutoConfigureMockMvc
@SpringBootTest
public class InstructorControllerFinalGradesSystemTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Test
    public void addGrades() throws Exception {

        // Final grades
        MockHttpServletResponse response;

        String gradeBefore1 = enrollmentRepository.findById(1)
                .map(Enrollment::getGrade)
                .orElse(null);
        //System.out.println(gradeBefore1);

        String gradeBefore2 = enrollmentRepository.findById(2)
                .map(Enrollment::getGrade)
                .orElse(null);
        //System.out.println(gradeBefore2);

        String gradeBefore3 = enrollmentRepository.findById(3)
                .map(Enrollment::getGrade)
                .orElse(null);
        //System.out.println(gradeBefore3);

        // create list with changed grades.
        List<EnrollmentDTO> gradeList = new ArrayList<>();
        //Add Enrollment
        gradeList.add(new EnrollmentDTO(1, "D", 3, "thomas edison", "tedison@csumb.edu", "cst338",
                "quiz", 1, 1, "52", "100", "M W 10:00-11:50",
                3, 2023, "Fall"));
        gradeList.add(new EnrollmentDTO(2, "F", 3, "thomas edison", "tedison@csumb.edu", "cst363",
                "quiz", 1, 1, "052", "104", "M W 10:00-11:50",
                4, 4, "Spring"));
        gradeList.add(new EnrollmentDTO(3, "B", 3, "thomas edison", "tedison@csumb.edu", "cst438",
                "quiz", 2, 1, "040", "100", "T Th 12:00-1:50",
                2, 4, "Spring"));

        response = mvc.perform(
                        MockMvcRequestBuilders
                                .put("/enrollments")
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(gradeList)))
                .andReturn()
                .getResponse();

        // check the response
        assertEquals(200, response.getStatus());

        // confirm that the grades were changed
        String gradeAfter1 = enrollmentRepository.findById(1)
                .map(Enrollment::getGrade)
                .orElse(null);
        assertEquals("D", gradeAfter1);

        String gradeAfter2 = enrollmentRepository.findById(2)
                .map(Enrollment::getGrade)
                .orElse(null);
        assertEquals("F", gradeAfter2);

        String gradeAfter3 = enrollmentRepository.findById(3)
                .map(Enrollment::getGrade)
                .orElse(null);
        assertEquals("B", gradeAfter3);

        //restore original data
        List<EnrollmentDTO> listCleanup = new ArrayList<>();
        listCleanup.add(new EnrollmentDTO(1, gradeBefore1, 3, "thomas edison", "tedison@csumb.edu", "cst338",
                "quiz", 1, 1, "52", "100", "M W 10:00-11:50",
                3, 2023, "Fall"));

        listCleanup.add(new EnrollmentDTO(2, gradeBefore2, 3, "thomas edison", "tedison@csumb.edu", "cst363",
                "quiz", 1, 1, "052", "104", "M W 10:00-11:50",
                4, 4, "Spring"));
        listCleanup.add(new EnrollmentDTO(3, gradeBefore3, 3, "thomas edison", "tedison@csumb.edu", "cst438",
                "quiz", 2, 1, "040", "100", "T Th 12:00-1:50",
                2, 4, "Spring"));

        response = mvc.perform(
                        MockMvcRequestBuilders
                                .put("/enrollments")
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(listCleanup)))
                .andReturn()
                .getResponse();

        // check the response
        assertEquals(200, response.getStatus());

        // Check if the grades returned to original values
        String gradeRestore1 = enrollmentRepository.findById(1)
                .map(Enrollment::getGrade)
                .orElse(null);
        assertEquals(gradeBefore1, gradeRestore1);

        String gradeRestore2 = enrollmentRepository.findById(2)
                .map(Enrollment::getGrade)
                .orElse(null);
        assertEquals(gradeBefore2, gradeRestore2);

        String gradeRestore3 = enrollmentRepository.findById(3)
                .map(Enrollment::getGrade)
                .orElse(null);
        assertEquals(gradeBefore3, gradeRestore3);
    }
}
