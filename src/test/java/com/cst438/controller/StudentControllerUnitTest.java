package com.cst438.controller;

import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.dto.EnrollmentDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureMockMvc
@SpringBootTest
public class StudentControllerUnitTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    EnrollmentRepository enrollmentRepository;


    @Test
    public void addEnrollment() throws Exception {
        MockHttpServletResponse response;

        EnrollmentDTO enrollment = new EnrollmentDTO(
                0,
                null,
                3,
                "thomas edison",
                "tedison@csumb.edu",
                "cst532",
                2,
                11,
                "052",
                "230",
                "M W 3:00-4:50",
                4,
                2024,
                "Fall"
        );

        // issue a http POST request to SpringTestServer
        // specify MediaType for request and response data
        // convert section to String data and set as request content
        response = mvc.perform(
                        MockMvcRequestBuilders
                                .post("/enrollments/sections/11")
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(enrollment)))
                .andReturn()
                .getResponse();
        // check the response code for 200 meaning OK
        assertEquals(200, response.getStatus());

        // return data converted from String to DTO
        EnrollmentDTO result = fromJsonString(response.getContentAsString(), EnrollmentDTO.class);

        // primary key should have a non zero value from the database
        assertNotEquals(0, result.enrollmentId());

        // check other fields of the DTO for expected values
        assertEquals("cst532", result.courseId());

        // check the database
        Enrollment e = enrollmentRepository.findById(result.enrollmentId()).orElse(null);
        assertNotNull(e);
        assertEquals("cst532", e.getSection().getCourse().getCourseId());

        // clean up after test. issue http DELETE request for section
        response = mvc.perform(
                        MockMvcRequestBuilders
                                .delete("/enrollments/"+result.enrollmentId()))
                .andReturn()
                .getResponse();

        assertEquals(200, response.getStatus());

        // check database for delete
        e = enrollmentRepository.findById(result.enrollmentId()).orElse(null);
        assertNull(e);  // section should not be found after delete
    }

    @Test
    public void addExistingEnroll() throws Exception {

        MockHttpServletResponse response;

        // Student is already enrolled in cst338
        EnrollmentDTO enrollment = new EnrollmentDTO(
                0,
                null,
                3,
                "thomas edison",
                "tedison@csumb.edu",
                "cst338",
                1,
                1,
                "052",
                "100",
                "M W 10:00-11:50",
                4,
                2024,
                "Spring"
        );

        // issue a http POST request to SpringTestServer
        // specify MediaType for request and response data
        // convert section to String data and set as request content
        response = mvc.perform(
                        MockMvcRequestBuilders
                                .post("/enrollments/sections/1")
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(enrollment)))
                .andReturn()
                .getResponse();

        // check the response code for 400, BAD_REQUEST
        assertEquals(400, response.getStatus());

        // check the expected error message
        String message = response.getErrorMessage();
        assertEquals("Student already enrolled", message);
    }

    @Test
    public void addInvalidSectionEnroll() throws Exception {

        MockHttpServletResponse response;

        //courseId cst672 does not exist
        EnrollmentDTO enrollment = new EnrollmentDTO(
                0,
                null,
                3,
                "thomas edison",
                "tedison@csumb.edu",
                "cst672",
                12,
                1,
                "052",
                "100",
                "M W 10:00-11:50",
                4,
                2024,
                "Spring"
        );

        // issue a http POST request to SpringTestServer
        // specify MediaType for request and response data
        // convert section to String data and set as request content
        response = mvc.perform(
                        MockMvcRequestBuilders
                                .post("/enrollments/sections/12")
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(enrollment)))
                .andReturn()
                .getResponse();

        // response should be 404, NOT_FOUND
        assertEquals(404, response.getStatus());

        // check the expected error message
        String message = response.getErrorMessage();
        assertEquals("section number not found", message);
    }

    @Test
    public void addExpiredDeadlineEnroll() throws Exception {

        MockHttpServletResponse response;

        // Add deadline has expired for section 1 cst438 (2023-08-30)
        EnrollmentDTO enrollment = new EnrollmentDTO(
                0,
                null,
                3,
                "thomas edison",
                "tedison@csumb.edu",
                "cst438",
                1,
                5,
                "052",
                "222",
                "T Th 12:00-1:50",
                4,
                2023,
                "Fall"
        );

        // issue a http POST request to SpringTestServer
        // specify MediaType for request and response data
        // convert section to String data and set as request content
        response = mvc.perform(
                        MockMvcRequestBuilders
                                .post("/enrollments/sections/5")
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(enrollment)))
                .andReturn()
                .getResponse();

        // check the response code for 400, BAD_REQUEST
        assertEquals(400, response.getStatus());

        // check the expected error message
        String message = response.getErrorMessage();
        assertEquals("Not within the enrollment period", message);
    }

    @Test
    public void updateFinalGrades() throws Exception {
        MockHttpServletResponse response;

        EnrollmentDTO enrollment1 = new EnrollmentDTO(
                3,
                "D",
                3,
                "thomas edison",
                "tedison@csumb.edu",
                "cst438",
                2,
                10,
                "052",
                "222",
                "T Th 12:00-1:50",
                4,
                2024,
                "Spring"
        );

        EnrollmentDTO enrollment2 = new EnrollmentDTO(
                4,
                "F",
                5,
                "ben franklin",
                "bfranklin@csumb.edu",
                "cst438",
                2,
                10,
                "052",
                "222",
                "T Th 12:00-1:50",
                4,
                2024,
                "Spring"
        );

        ArrayList enrollList = new ArrayList<>();
        enrollList.add(enrollment1);
        enrollList.add(enrollment2);

        // issue a http PUT request to SpringTestServer
        // specify MediaType for request and response data
        // convert section to String data and set as request content
        response = mvc.perform(
                        MockMvcRequestBuilders
                                .put("/enrollments")
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(enrollList)))
                .andReturn()
                .getResponse();
        // check the response code for 200 meaning OK
        assertEquals(200, response.getStatus());
    }

    @Test
    public void deleteEnrollAfterDropDeadline() throws Exception {

        MockHttpServletResponse response;

        // A check to verify that an enrollment cannot be
        // dropped after the drop deadline.
        // A hard enrollmentId (1) is used to test.
        response = mvc.perform(
                        MockMvcRequestBuilders
                                .delete("/enrollments/"+1))
                .andReturn()
                .getResponse();

        // check the response code for 400, BAD_REQUEST
        assertEquals(400, response.getStatus());

        // check the expected error message
        String message = response.getErrorMessage();
        assertEquals("Cannot drop course after the drop deadline", message);

    }

    private static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T  fromJsonString(String str, Class<T> valueType ) {
        try {
            return new ObjectMapper().readValue(str, valueType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

