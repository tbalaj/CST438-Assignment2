package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.EnrollmentDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.sql.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;


@ExtendWith(MockitoExtension.class)
public class StudentControllerUnitTest {

    @InjectMocks
    private StudentController studentController;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private TermRepository termRepository;

    @Mock
    private GradeRepository gradeRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    //Testing enrollment
    @Test
    public void addCourse_ValidScenario_ReturnsEnrollmentDTO() {
        // Given
        int studentId = 1;
        int sectionNo = 10;

        // Mock data for term
        Term mockTerm = new Term();
        mockTerm.setTermId(10);
        mockTerm.setAddDate(new Date(System.currentTimeMillis() - 100000)); // Past date
        mockTerm.setAddDeadline(new Date(System.currentTimeMillis() + 100000)); // Future date
        mockTerm.setYear(2024);
        mockTerm.setSemester("Fall");

        // Mock data for course
        Course mockCourse = new Course();
        mockCourse.setCourseId("cst438");
        mockCourse.setTitle("Software Engineering");
        mockCourse.setCredits(4);

        // Mock data for section
        Section mockSection = new Section();
        mockSection.setTerm(mockTerm);
        mockSection.setCourse(mockCourse);
        mockSection.setSecId(1);
        mockSection.setSectionNo(sectionNo);
        mockSection.setBuilding("Building A");
        mockSection.setRoom("Room 101");
        mockSection.setTimes("10:00 AM - 11:00 AM");

        // Mock data for user
        User mockUser = new User();
        mockUser.setId(studentId);
        mockUser.setName("John Doe");
        mockUser.setEmail("john.doe@example.com");

        // Mock data for enrollment
        Enrollment mockEnrollment = new Enrollment();
        mockEnrollment.setEnrollmentId(1);
        mockEnrollment.setGrade(null);
        mockEnrollment.setUser(mockUser);
        mockEnrollment.setSection(mockSection);

        // Mocking repository methods with lenient
        lenient().when(sectionRepository.findById(sectionNo)).thenReturn(Optional.of(mockSection));
        lenient().when(termRepository.existsById(anyInt())).thenReturn(true);
        lenient().when(userRepository.findById(studentId)).thenReturn(Optional.of(mockUser));
        lenient().when(enrollmentRepository.findEnrollmentBySectionNoAndStudentId(sectionNo, studentId)).thenReturn(null);
        lenient().when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(mockEnrollment); // Mock save


        EnrollmentDTO result = studentController.addCourse(sectionNo, studentId);


        assertNotNull(result);
        assertEquals(studentId, result.studentId());
        assertEquals("John Doe", result.name());
        assertEquals("john.doe@example.com", result.email());
        assertEquals("cst438", result.courseId());
        assertEquals("Software Engineering", result.title());
        assertEquals(4, result.credits());
        assertEquals(sectionNo, result.sectionNo());
        assertEquals("Building A", result.building());
        assertEquals("Room 101", result.room());
        assertEquals("10:00 AM - 11:00 AM", result.times());
        assertEquals(2024, result.year());
        assertEquals("Fall", result.semester());
    }




    @Test
    void addCourse_UserNotFound_ThrowsException() {
        when(userRepository.findById(anyInt())).thenReturn(Optional.empty());

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class, () -> {
            studentController.addCourse(123, 1);
        });

        assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
        assertEquals("User not found", thrown.getReason());
    }

    @Test
    void addCourse_SectionNotFound_ThrowsException() {
        User user = new User();
        user.setId(1);
        user.setType("STUDENT");

        when(userRepository.findById(anyInt())).thenReturn(Optional.of(user));
        when(sectionRepository.findById(anyInt())).thenReturn(Optional.empty());

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class, () -> {
            studentController.addCourse(123, 1);
        });

        assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
        assertEquals("Section not valid", thrown.getReason());
    }


    //Testing already enrolled
    @Test
    public void addCourse_AlreadyEnrolled_ThrowsException() {
        // Given
        int studentId = 3;
        int sectionNo = 10;

        // Mock data for term
        Term mockTerm = new Term();
        mockTerm.setTermId(10);
        mockTerm.setAddDate(new Date(System.currentTimeMillis() - 100000));
        mockTerm.setAddDeadline(new Date(System.currentTimeMillis() + 100000));

        // Mock data for course
        Course mockCourse = new Course();
        mockCourse.setCourseId("cst438");
        mockCourse.setTitle("Software Engineering");
        mockCourse.setCredits(4);

        // Mock data for section
        Section mockSection = new Section();
        mockSection.setTerm(mockTerm);
        mockSection.setCourse(mockCourse);

        // Mock existing enrollment
        Enrollment existingEnrollment = new Enrollment();
        existingEnrollment.setEnrollmentId(1);
        existingEnrollment.setUser(new User());
        existingEnrollment.setSection(mockSection);

        // Mocking repository methods
        lenient().when(sectionRepository.findById(sectionNo)).thenReturn(Optional.of(mockSection));
        lenient().when(termRepository.existsById(mockTerm.getTermId())).thenReturn(true);
        lenient().when(userRepository.findById(studentId)).thenReturn(Optional.of(new User()));
        lenient().when(enrollmentRepository.findEnrollmentBySectionNoAndStudentId(sectionNo, studentId)).thenReturn(existingEnrollment);

        ResponseStatusException thrownException = assertThrows(ResponseStatusException.class, () -> {
            studentController.addCourse(sectionNo, studentId);
        });

        assertEquals(HttpStatus.BAD_REQUEST, thrownException.getStatusCode());
        assertEquals("Already enrolled", thrownException.getReason());
    }

}
