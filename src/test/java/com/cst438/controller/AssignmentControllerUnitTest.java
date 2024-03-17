package com.cst438.controller;

import com.cst438.domain.Assignment;
import com.cst438.domain.AssignmentRepository;
import com.cst438.dto.AssignmentDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Date;

/*
 * example of unit test to add a section to an existing course
 */

@AutoConfigureMockMvc
@SpringBootTest
public class AssignmentControllerUnitTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AssignmentRepository assignmentRepository;

    @Test
    public void createAssignment() throws Exception {
    	 MockHttpServletResponse response;

        // 
    	 AssignmentDTO assignment = new AssignmentDTO(
    			 1,
                 "Test Assignment",
                 Date.valueOf("2001-01-01"),
                 "TestID",
                 1,
                 1
         );
    	 
    	 response = mvc.perform(
                 MockMvcRequestBuilders
                         .post("/assignments")
                         .accept(MediaType.APPLICATION_JSON)
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(asJsonString(assignment)))
                 .andReturn()
                 .getResponse();
    	 // check the response code for 200 meaning OK
         assertEquals(200, response.getStatus());

         // return data converted from String to DTO
         AssignmentDTO result = fromJsonString(response.getContentAsString(), AssignmentDTO.class);

         // primary key should have a non zero value from the database
         assertNotEquals(0, result.secId());
         // check other fields of the DTO for expected values
         assertNotEquals(0,result.secNo());
         assertEquals("Test Assignment",result.title());

    }
    
    public void failCreateAssignment() throws Exception {
   	 MockHttpServletResponse response;

       //pass in 0 as the section id - an invalid ID number
   	 AssignmentDTO assignment = new AssignmentDTO(
   			 1,
                "Test Assignment",
                Date.valueOf("2001-01-01"),
                "TestID",
                0,
                1
        );
   	 
   	 response = mvc.perform(
                MockMvcRequestBuilders
                        .post("/assignments")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(assignment)))
                .andReturn()
                .getResponse();
   	 // check the response code for 400 meaning FAIL
        assertEquals(404, response.getStatus());
        
        String message = response.getErrorMessage();
        assertEquals("Section not found", message);
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