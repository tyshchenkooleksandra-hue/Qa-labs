package com.softserve.service;

import com.softserve.dto.SemesterWithGroupsDTO;
import com.softserve.dto.TeacherDTO;
import com.softserve.entity.*;
import com.softserve.entity.enums.EvenOdd;
import com.softserve.exception.EntityNotFoundException;
import com.softserve.repository.ScheduleRepository;
import com.softserve.service.impl.ScheduleCacheService;
import com.softserve.service.impl.ScheduleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.mail.MessagingException;
import java.time.DayOfWeek;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private ScheduleCacheService cacheService;
    @Mock
    private LessonService lessonService;
    @Mock
    private TeacherService teacherService;
    @Mock
    private SemesterService semesterService;
    @Mock
    private MailService mailService;

    @InjectMocks
    private ScheduleServiceImpl scheduleServiceImpl;

    private Lesson lesson;
    private Period period;
    private Room room;
    private Schedule schedule;

    @BeforeEach
    void setUp() {
        Teacher teacher = new Teacher();
        teacher.setId(1L);

        Group group = new Group();

        Semester semester = new Semester();
        semester.setId(4L);

        lesson = new Lesson();
        lesson.setId(1L);
        lesson.setSemester(semester);
        lesson.setGroup(group);
        lesson.setTeacher(teacher);

        period = new Period();

        room = new Room();
        room.setId(1L);
        room.setName("Room1");

        schedule = new Schedule();
        schedule.setId(1L);
        schedule.setEvenOdd(EvenOdd.ODD);
        schedule.setLesson(lesson);
        schedule.setPeriod(period);
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        schedule.setRoom(room);
    }

    @Test
    void getById() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        Schedule result = scheduleServiceImpl.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(schedule.getId());
        verify(scheduleRepository, times(1)).findById(1L);
    }

    @Test
    void throwEntityNotFoundExceptionIfScheduleNotFound() {
        when(scheduleRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> scheduleServiceImpl.getById(2L));

        verify(scheduleRepository, times(1)).findById(2L);
    }

    @Test
    void save() {
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(schedule);

        Schedule result = scheduleServiceImpl.save(schedule);

        assertThat(result).isNotNull();
        verify(scheduleRepository, times(1)).save(schedule);
    }

    @Test
    void update() {
        Schedule updatedSchedule = new Schedule();
        updatedSchedule.setId(1L);
        updatedSchedule.setEvenOdd(EvenOdd.EVEN);
        updatedSchedule.setLesson(lesson);
        updatedSchedule.setPeriod(period);
        updatedSchedule.setDayOfWeek(DayOfWeek.MONDAY);
        updatedSchedule.setRoom(room);

        when(scheduleRepository.update(updatedSchedule)).thenReturn(updatedSchedule);

        Schedule result = scheduleServiceImpl.update(updatedSchedule);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(updatedSchedule);
        verify(scheduleRepository, times(1)).update(updatedSchedule);
    }

    @Test
    void sendScheduleToTeachers() throws MessagingException, jakarta.mail.MessagingException {
        TeacherDTO teacherDTO = new TeacherDTO();
        teacherDTO.setId(10L);
        teacherDTO.setName("John");
        teacherDTO.setSurname("Doe");
        teacherDTO.setPatronymic("Smith");
        teacherDTO.setPosition("Professor");
        teacherDTO.setEmail("test@gmail.com");

        SemesterWithGroupsDTO semesterWithGroupsDTO = new SemesterWithGroupsDTO();
        semesterWithGroupsDTO.setId(4L);

        List<DayOfWeek> dayOfWeeks = new ArrayList<>(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY));
        Long[] ids = {10L, 20L, 30L, 40L};

        when(teacherService.getById(anyLong())).thenReturn(teacherDTO);
        when(semesterService.getById(anyLong())).thenReturn(semesterWithGroupsDTO);
        when(scheduleRepository.getDaysWhenTeacherHasClassesBySemester(anyLong(), anyLong())).thenReturn(dayOfWeeks);
        doNothing().when(mailService).send(anyString(), anyString(), anyString(), anyString(), any());

        scheduleServiceImpl.sendScheduleToTeachers(4L, ids, Locale.ENGLISH);

        verify(mailService, times(ids.length)).send(anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void scheduleForGroupedLessons() {
        Group group2 = new Group();

        Lesson lesson2 = new Lesson();
        lesson2.setId(2L);
        lesson2.setSemester(lesson.getSemester());
        lesson2.setGroup(group2);
        lesson2.setGrouped(true);

        lesson.setGrouped(true);

        when(lessonService.getAllGroupedLessonsByLesson(lesson)).thenReturn(Arrays.asList(lesson2, lesson));

        List<Schedule> schedules = scheduleServiceImpl.schedulesForGroupedLessons(schedule);

        assertThat(schedules).hasSize(2);
    }

    @Test
    void getAllOrderedByRoomsDaysPeriodsTest() {
        Room room2 = new Room();
        room2.setId(2L);

        Schedule schedule2 = new Schedule();
        schedule2.setId(2L);
        schedule2.setEvenOdd(EvenOdd.ODD);
        schedule2.setDayOfWeek(DayOfWeek.MONDAY);
        schedule2.setRoom(room2);

        when(scheduleRepository.getAllOrdered(1L)).thenReturn(List.of(schedule, schedule2));

        Map<Room, List<Schedule>> expected = new HashMap<>();
        expected.put(room, List.of(schedule));
        expected.put(room2, List.of(schedule2));

        Map<Room, List<Schedule>> actual = scheduleServiceImpl.getAllOrdered(1L);

        assertThat(actual).isEqualTo(expected);
        verify(scheduleRepository, times(1)).getAllOrdered(1L);
    }
}
