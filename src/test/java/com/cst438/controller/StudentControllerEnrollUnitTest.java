package com.cst438.controller;

import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureMockMvc
@SpringBootTest
public class StudentControllerEnrollUnitTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    EnrollmentRepository enrollmentRepository;

    //  8.	student attempts to enroll in a section but the section number is invalid
    //

    @Test
    public void addEnrollmentBadSecNo() throws Exception {

        MockHttpServletResponse response;

        int secNo = -1;
        final int studentId = 3;

        response = mvc.perform(
                        MockMvcRequestBuilders
                                .post("/enrollments/sections/{sectionNo}", secNo)
                                .param("studentId", String.valueOf(studentId))
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse();

        System.out.println(MockMvcRequestBuilders
                .post("/enrollments/sections/{sectionNo}", secNo)
                .param("studentId", String.valueOf(studentId))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).toString());


        assertEquals(400, response.getStatus());
        String message = response.getErrorMessage();

        assertEquals("section not valid", message);


        Enrollment e = enrollmentRepository.findEnrollmentBySectionNoAndStudentId(secNo, studentId);

        try {
            if (nonNull(e)) {

                response = mvc.perform(
                                MockMvcRequestBuilders
                                        .delete("/enrollments/" + e.getEnrollmentId()))
                        .andReturn()
                        .getResponse();
                assertEquals(400, response.getStatus());
                e = enrollmentRepository.findEnrollmentBySectionNoAndStudentId(secNo, studentId);
            }
        } catch (Exception ex) {
            System.err.println("Error deleting enrollment: " + ex.getMessage());
        }

        assertNull(e);
    }


    // 9.	student attempts to enroll in a section but it is past the add deadline.
    @Test
    public void addEnrollmentPastAddDeadline() throws Exception {

        MockHttpServletResponse response;

        int secNo = 2;
        final int studentId = 3;

        Enrollment e = enrollmentRepository.findEnrollmentBySectionNoAndStudentId(secNo,studentId);
        assertNull(e);

        response = mvc.perform(
                        MockMvcRequestBuilders
                                .post("/enrollments/sections/{sectionNo}", secNo)
                                .param("studentId", String.valueOf(studentId))
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse();

        assertEquals(400, response.getStatus());

        String message = response.getErrorMessage();
        assertEquals("You missed the dead line!", message);

        e = enrollmentRepository.findEnrollmentBySectionNoAndStudentId(secNo, studentId);

        if(nonNull(e)) {

            response = mvc.perform(
                            MockMvcRequestBuilders
                                    .delete("/sections/"+e.getEnrollmentId()))
                    .andReturn()
                    .getResponse();

            assertEquals(200, response.getStatus());
            e = enrollmentRepository.findEnrollmentBySectionNoAndStudentId(secNo, studentId);

        }

        assertNull(e);
    }
}
