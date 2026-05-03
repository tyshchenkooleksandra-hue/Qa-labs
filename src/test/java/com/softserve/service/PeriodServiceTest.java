package com.softserve.service;

import com.softserve.dto.AddPeriodDTO;
import com.softserve.dto.PeriodDTO;
import com.softserve.entity.Period;
import com.softserve.exception.EntityNotFoundException;
import com.softserve.exception.FieldAlreadyExistsException;
import com.softserve.exception.IncorrectTimeException;
import com.softserve.exception.PeriodConflictException;
import com.softserve.mapper.PeriodMapper;
import com.softserve.repository.PeriodRepository;
import com.softserve.service.impl.PeriodServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class PeriodServiceTest {

    @Mock
    private PeriodRepository periodRepository;
    @Mock
    private PeriodMapper periodMapper;

    @InjectMocks
    private PeriodServiceImpl periodService;

    private Period period;
    private PeriodDTO periodDTO;
    private AddPeriodDTO addPeriodDTO;

    @BeforeEach
    void setUp() {
        period = new Period();
        period.setId(1L);
        period.setName("Some period");
        period.setStartTime(LocalTime.parse("03:00:00"));
        period.setEndTime(LocalTime.parse("04:00:00"));

        periodDTO = new PeriodDTO();
        periodDTO.setId(1L);
        periodDTO.setName("Some period");
        periodDTO.setStartTime(LocalTime.parse("03:00:00"));
        periodDTO.setEndTime(LocalTime.parse("04:00:00"));

        addPeriodDTO = new AddPeriodDTO();
        addPeriodDTO.setName("Some period");
        addPeriodDTO.setStartTime(LocalTime.parse("03:00:00"));
        addPeriodDTO.setEndTime(LocalTime.parse("04:00:00"));
    }

    @Test
    void getById() {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(periodMapper.convertToDto(period)).thenReturn(periodDTO);

        PeriodDTO result = periodService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result).usingRecursiveComparison().isEqualTo(periodDTO);
        verify(periodRepository, times(1)).findById(1L);
        verify(periodMapper, times(1)).convertToDto(period);
    }

    @Test
    void throwEntityNotFoundExceptionIfPeriodNotFoundById() {
        when(periodRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> periodService.getById(2L));

        verify(periodRepository, times(1)).findById(2L);
    }

    @Test
    void savePeriodIfItHasCorrectTimeAndNotConflictsWithOtherPeriodsAndNameIsNotExist() {
        Period anotherPeriod = new Period();
        anotherPeriod.setId(2L);
        anotherPeriod.setName("Another period");
        anotherPeriod.setStartTime(LocalTime.parse("05:00:00"));
        anotherPeriod.setEndTime(LocalTime.parse("06:00:00"));

        when(periodRepository.findByName(addPeriodDTO.getName())).thenReturn(Optional.empty());
        when(periodMapper.convertToEntity(addPeriodDTO)).thenReturn(period);
        when(periodRepository.getAll()).thenReturn(List.of(anotherPeriod));
        when(periodRepository.save(period)).thenReturn(period);
        when(periodMapper.convertToDto(period)).thenReturn(periodDTO);

        PeriodDTO result = periodService.save(addPeriodDTO);

        assertThat(result).isNotNull();
        assertThat(result).usingRecursiveComparison().isEqualTo(periodDTO);
        verify(periodRepository, times(1)).getAll();
        verify(periodRepository, times(1)).save(period);
    }

    @Test
    void throwIncorrectTimeExceptionIfSavePeriodBeginsAfterHisEnd() {
        addPeriodDTO.setStartTime(LocalTime.parse("05:00:00"));
        addPeriodDTO.setEndTime(LocalTime.parse("04:00:00"));

        assertThrows(IncorrectTimeException.class, () -> periodService.save(addPeriodDTO));

        verify(periodRepository, never()).save(any());
    }

    @Test
    void throwFieldAlreadyExistsExceptionIfSavePeriodWithExistingName() {
        Period existingPeriod = new Period();
        existingPeriod.setId(2L);
        existingPeriod.setName("Some period");

        when(periodRepository.findByName(addPeriodDTO.getName())).thenReturn(Optional.of(existingPeriod));

        assertThrows(FieldAlreadyExistsException.class, () -> periodService.save(addPeriodDTO));

        verify(periodRepository, never()).save(any());
    }

    @Test
    void throwPeriodConflictExceptionIfSavedPeriodConflictsWithOtherPeriods() {
        Period conflictingPeriod = new Period();
        conflictingPeriod.setId(2L);
        conflictingPeriod.setName("Another period");
        conflictingPeriod.setStartTime(LocalTime.parse("02:00:00"));
        conflictingPeriod.setEndTime(LocalTime.parse("03:00:00"));

        when(periodRepository.findByName(addPeriodDTO.getName())).thenReturn(Optional.empty());
        when(periodMapper.convertToEntity(addPeriodDTO)).thenReturn(period);
        when(periodRepository.getAll()).thenReturn(List.of(conflictingPeriod));

        assertThrows(PeriodConflictException.class, () -> periodService.save(addPeriodDTO));

        verify(periodRepository, never()).save(any());
    }

    @Test
    void saveListOfPeriodsIfAllPeriodsHaveCorrectTimeAndNotConflictWithOtherPeriods() {
        AddPeriodDTO anotherAddPeriodDTO = new AddPeriodDTO();
        anotherAddPeriodDTO.setName("Another period");
        anotherAddPeriodDTO.setStartTime(LocalTime.parse("01:00:00"));
        anotherAddPeriodDTO.setEndTime(LocalTime.parse("02:00:00"));

        Period anotherPeriod = new Period();
        anotherPeriod.setId(2L);
        anotherPeriod.setName("Another period");
        anotherPeriod.setStartTime(LocalTime.parse("01:00:00"));
        anotherPeriod.setEndTime(LocalTime.parse("02:00:00"));

        PeriodDTO anotherPeriodDTO = new PeriodDTO();
        anotherPeriodDTO.setId(2L);
        anotherPeriodDTO.setName("Another period");

        List<AddPeriodDTO> addPeriodDTOs = List.of(addPeriodDTO, anotherAddPeriodDTO);

        when(periodMapper.convertToEntityList(addPeriodDTOs)).thenReturn(List.of(period, anotherPeriod));
        when(periodRepository.getAll()).thenReturn(new ArrayList<>());
        when(periodRepository.save(period)).thenReturn(period);
        when(periodRepository.save(anotherPeriod)).thenReturn(anotherPeriod);
        when(periodMapper.convertToDtoList(List.of(period, anotherPeriod))).thenReturn(List.of(periodDTO, anotherPeriodDTO));

        List<PeriodDTO> result = periodService.saveAll(addPeriodDTOs);

        assertThat(result).isNotNull().hasSize(2);
        verify(periodRepository, times(2)).save(any(Period.class));
    }

    @Test
    void throwIncorrectTimeExceptionIfSaveListOfPeriodsAndOneHasIncorrectTime() {
        AddPeriodDTO invalidDTO = new AddPeriodDTO();
        invalidDTO.setName("Invalid period");
        invalidDTO.setStartTime(LocalTime.parse("05:00:00"));
        invalidDTO.setEndTime(LocalTime.parse("04:00:00"));

        assertThrows(IncorrectTimeException.class, () -> periodService.saveAll(List.of(invalidDTO)));

        verify(periodRepository, never()).save(any());
    }

    @Test
    void throwPeriodConflictExceptionIfSaveListOfPeriodsAndOneConflictsWithExisting() {
        Period conflictingPeriod = new Period();
        conflictingPeriod.setId(2L);
        conflictingPeriod.setName("Another period");
        conflictingPeriod.setStartTime(LocalTime.parse("03:00:00"));
        conflictingPeriod.setEndTime(LocalTime.parse("04:00:00"));

        when(periodMapper.convertToEntityList(List.of(addPeriodDTO))).thenReturn(List.of(period));
        when(periodRepository.getAll()).thenReturn(List.of(conflictingPeriod));

        assertThrows(PeriodConflictException.class, () -> periodService.saveAll(List.of(addPeriodDTO)));

        verify(periodRepository, never()).save(any());
    }

    @Test
    void updatePeriodIfItHasCorrectTimeAndNotConflictsWithOtherPeriodsAndNameIsNotExist() {
        Period anotherPeriod = new Period();
        anotherPeriod.setId(2L);
        anotherPeriod.setName("Another period");
        anotherPeriod.setStartTime(LocalTime.parse("06:00:00"));
        anotherPeriod.setEndTime(LocalTime.parse("07:00:00"));

        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(periodRepository.findByName(periodDTO.getName())).thenReturn(Optional.of(period));
        when(periodMapper.convertToEntity(periodDTO)).thenReturn(period);
        when(periodRepository.getAll()).thenReturn(List.of(anotherPeriod));
        when(periodRepository.update(period)).thenReturn(period);
        when(periodMapper.convertToDto(period)).thenReturn(periodDTO);

        PeriodDTO result = periodService.update(periodDTO);

        assertThat(result).isNotNull();
        assertThat(result).usingRecursiveComparison().isEqualTo(periodDTO);
        verify(periodRepository, times(1)).update(period);
        verify(periodRepository, times(1)).findById(1L);
    }

    @Test
    void throwFieldAlreadyExistsExceptionIfUpdatedPeriodNameAlreadyExists() {
        Period existingPeriod = new Period();
        existingPeriod.setId(2L);
        existingPeriod.setName("Some period");

        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(periodRepository.findByName(periodDTO.getName())).thenReturn(Optional.of(existingPeriod));

        assertThrows(FieldAlreadyExistsException.class, () -> periodService.update(periodDTO));

        verify(periodRepository, never()).update(any());
    }

    @Test
    void throwIncorrectTimeExceptionIfUpdatedPeriodBeginsAfterHisEnd() {
        periodDTO.setStartTime(LocalTime.parse("05:00:00"));
        periodDTO.setEndTime(LocalTime.parse("04:00:00"));

        assertThrows(IncorrectTimeException.class, () -> periodService.update(periodDTO));

        verify(periodRepository, never()).update(any());
    }

    @Test
    void throwPeriodConflictExceptionIfUpdatedPeriodConflictsWithOther() {
        Period conflictingPeriod = new Period();
        conflictingPeriod.setId(2L);
        conflictingPeriod.setName("Another period");
        conflictingPeriod.setStartTime(LocalTime.parse("03:00:00"));
        conflictingPeriod.setEndTime(LocalTime.parse("04:00:00"));

        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(periodRepository.findByName(periodDTO.getName())).thenReturn(Optional.of(period));
        when(periodMapper.convertToEntity(periodDTO)).thenReturn(period);
        when(periodRepository.getAll()).thenReturn(List.of(conflictingPeriod));

        assertThrows(PeriodConflictException.class, () -> periodService.update(periodDTO));

        verify(periodRepository, never()).update(any());
    }
}
