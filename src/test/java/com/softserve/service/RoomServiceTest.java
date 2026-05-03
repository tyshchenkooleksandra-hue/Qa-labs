package com.softserve.service;

import com.softserve.dto.RoomDTO;
import com.softserve.entity.Room;
import com.softserve.entity.RoomType;
import com.softserve.exception.EntityAlreadyExistsException;
import com.softserve.exception.EntityNotFoundException;
import com.softserve.exception.SortOrderNotExistsException;
import com.softserve.mapper.RoomForScheduleInfoMapper;
import com.softserve.mapper.RoomMapper;
import com.softserve.repository.RoomRepository;
import com.softserve.repository.SortOrderRepository;
import com.softserve.service.impl.RoomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private RoomMapper roomMapper;
    @Mock
    private RoomForScheduleInfoMapper roomForScheduleInfoMapper;
    @Mock
    private SortOrderRepository<Room> sortOrderRepository;

    @InjectMocks
    private RoomServiceImpl roomService;

    private Room room;
    private RoomDTO roomDTO;

    @BeforeEach
    void setUp() {
        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setDescription("Small auditory");

        room = new Room();
        room.setId(1L);
        room.setName("1 Room");
        room.setType(roomType);

        roomDTO = new RoomDTO();
        roomDTO.setId(1L);
        roomDTO.setName("1 Room");
    }

    @Test
    void getById() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomMapper.convertToDto(room)).thenReturn(roomDTO);

        RoomDTO result = roomService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(roomDTO.getId());
        assertThat(result.getName()).isEqualTo(roomDTO.getName());
        verify(roomRepository, times(1)).findById(1L);
        verify(roomMapper, times(1)).convertToDto(room);
    }

    @Test
    void throwEntityNotFoundExceptionIfRoomNotFound() {
        when(roomRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> roomService.getById(2L));

        verify(roomRepository, times(1)).findById(2L);
    }

    @Test
    void saveRoomIfNameAndTypeAreNotExist() {
        when(roomMapper.convertToEntity(roomDTO)).thenReturn(room);
        when(roomRepository.countRoomDuplicates(room)).thenReturn(0L);
        when(roomRepository.getLastSortOrder()).thenReturn(Optional.of(0));
        when(roomRepository.save(room)).thenReturn(room);
        when(roomMapper.convertToDto(room)).thenReturn(roomDTO);

        RoomDTO result = roomService.save(roomDTO);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(roomDTO.getName());
        assertThat(room.getSortOrder()).isEqualTo(1);
        verify(roomRepository, times(1)).save(room);
        verify(roomRepository, times(1)).countRoomDuplicates(room);
    }

    @Test
    void throwEntityAlreadyExistsExceptionIfSavedRoomAlreadyExists() {
        when(roomMapper.convertToEntity(roomDTO)).thenReturn(room);
        when(roomRepository.countRoomDuplicates(room)).thenReturn(1L);

        assertThrows(EntityAlreadyExistsException.class, () -> roomService.save(roomDTO));

        verify(roomRepository, never()).save(any());
        verify(roomRepository, times(1)).countRoomDuplicates(room);
    }

    @Test
    void updateRoomIfNameAndTypeAreNotExist() {
        RoomDTO updatedRoomDTO = new RoomDTO();
        updatedRoomDTO.setId(1L);
        updatedRoomDTO.setName("1 Room updated");

        Room updatedRoom = new Room();
        updatedRoom.setId(1L);
        updatedRoom.setName("1 Room updated");

        when(roomMapper.convertToEntity(updatedRoomDTO)).thenReturn(updatedRoom);
        when(roomRepository.countRoomDuplicates(updatedRoom)).thenReturn(0L);
        when(sortOrderRepository.getSortOrderById(1L)).thenReturn(Optional.of(1));
        when(roomRepository.update(updatedRoom)).thenReturn(updatedRoom);
        when(roomMapper.convertToDto(updatedRoom)).thenReturn(updatedRoomDTO);

        RoomDTO result = roomService.update(updatedRoomDTO);

        assertThat(result).isNotNull();
        assertThat(result).usingRecursiveComparison().isEqualTo(updatedRoomDTO);
        assertThat(updatedRoom.getSortOrder()).isEqualTo(1);
        verify(roomRepository, times(1)).update(updatedRoom);
        verify(roomRepository, times(1)).countRoomDuplicates(updatedRoom);
    }

    @Test
    void throwEntityAlreadyExistsExceptionIfUpdatedNameAndTypeAlreadyExist() {
        RoomDTO updatedRoomDTO = new RoomDTO();
        updatedRoomDTO.setId(1L);
        updatedRoomDTO.setName("1 Room updated");

        Room updatedRoom = new Room();
        updatedRoom.setId(1L);
        updatedRoom.setName("1 Room updated");

        when(roomMapper.convertToEntity(updatedRoomDTO)).thenReturn(updatedRoom);
        when(roomRepository.countRoomDuplicates(updatedRoom)).thenReturn(1L);

        assertThrows(EntityAlreadyExistsException.class, () -> roomService.update(updatedRoomDTO));

        verify(roomRepository, never()).update(any());
        verify(roomRepository, times(1)).countRoomDuplicates(updatedRoom);
    }

    @Test
    void throwSortOrderNotExistsExceptionWhenCreateAfterNotExistRoom() {
        Long afterNotExistRoomId = 10L;

        when(roomMapper.convertToEntity(roomDTO)).thenReturn(room);
        when(sortOrderRepository.createAfterOrder(room, afterNotExistRoomId))
                .thenThrow(SortOrderNotExistsException.class);

        assertThrows(SortOrderNotExistsException.class,
                () -> roomService.createAfterOrder(roomDTO, afterNotExistRoomId));

        verify(sortOrderRepository, times(1)).createAfterOrder(room, afterNotExistRoomId);
    }

    @Test
    void createAfterOrderSuccessfully() {
        RoomDTO roomWithOrderDTO = new RoomDTO();
        roomWithOrderDTO.setName("11 Room");

        Room roomWithOrder = new Room();
        roomWithOrder.setName("11 Room");
        roomWithOrder.setSortOrder(1);

        Long afterId = 0L;

        when(roomMapper.convertToEntity(roomDTO)).thenReturn(room);
        when(sortOrderRepository.createAfterOrder(room, afterId)).thenReturn(roomWithOrder);
        when(roomMapper.convertToDto(roomWithOrder)).thenReturn(roomWithOrderDTO);

        RoomDTO result = roomService.createAfterOrder(roomDTO, afterId);

        assertThat(result).isNotNull();
        assertThat(result).usingRecursiveComparison().isEqualTo(roomWithOrderDTO);
        verify(sortOrderRepository, times(1)).createAfterOrder(room, afterId);
    }

    @Nested
    class DeleteById {
        @Test
        void deleteByIdSuccessfully() {
            // Arrange
            room.setSortOrder(2);
            when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
            when(roomRepository.delete(room)).thenReturn(room);
            when(roomMapper.convertToDto(room)).thenReturn(roomDTO);

            // Act
            RoomDTO result = roomService.deleteById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).usingRecursiveComparison().isEqualTo(roomDTO);
            verify(roomRepository, times(1)).findById(1L);
            verify(roomRepository, times(1)).delete(room);
            verify(roomRepository, times(1)).shiftSortOrderRange(
                    room.getSortOrder() + 1, null, RoomRepository.Direction.UP);
        }

        @Test
        void throwEntityNotFoundExceptionIfDeletedRoomNotFound() {
            // Arrange
            Long nonExistentId = 99L;
            when(roomRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(EntityNotFoundException.class, () -> roomService.deleteById(nonExistentId));
            verify(roomRepository, times(1)).findById(nonExistentId);
            verify(roomRepository, never()).delete(any());
            verify(roomRepository, never()).shiftSortOrderRange(any(), any(), any());
        }
    }

    @Nested
    class GetAll {
        @Test
        void getAllRoomsSuccessfully() {
            // Arrange
            List<Room> rooms = Collections.singletonList(room);
            List<RoomDTO> expected = Collections.singletonList(roomDTO);
            when(roomRepository.getAll()).thenReturn(rooms);
            when(roomMapper.convertToDtoList(rooms)).thenReturn(expected);

            // Act
            List<RoomDTO> result = roomService.getAll();

            // Assert
            assertThat(result).hasSize(1).containsExactlyElementsOf(expected);
            verify(roomRepository, times(1)).getAll();
            verify(roomMapper, times(1)).convertToDtoList(rooms);
        }

        @Test
        void getAllRoomsReturnsEmptyListWhenNoRoomsExist() {
            // Arrange
            when(roomRepository.getAll()).thenReturn(Collections.emptyList());
            when(roomMapper.convertToDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

            // Act
            List<RoomDTO> result = roomService.getAll();

            // Assert
            assertThat(result).isEmpty();
            verify(roomRepository, times(1)).getAll();
        }
    }

    @Nested
    class GetDisabled {
        @Test
        void getDisabledRoomsSuccessfully() {
            // Arrange
            List<Room> disabledRooms = Collections.singletonList(room);
            List<RoomDTO> expected = Collections.singletonList(roomDTO);
            when(roomRepository.getDisabled()).thenReturn(disabledRooms);
            when(roomMapper.convertToDtoList(disabledRooms)).thenReturn(expected);

            // Act
            List<RoomDTO> result = roomService.getDisabled();

            // Assert
            assertThat(result).hasSize(1).containsExactlyElementsOf(expected);
            verify(roomRepository, times(1)).getDisabled();
            verify(roomMapper, times(1)).convertToDtoList(disabledRooms);
        }

        @Test
        void getDisabledRoomsReturnsEmptyListWhenNoRoomsAreDisabled() {
            // Arrange
            when(roomRepository.getDisabled()).thenReturn(Collections.emptyList());
            when(roomMapper.convertToDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

            // Act
            List<RoomDTO> result = roomService.getDisabled();

            // Assert
            assertThat(result).isEmpty();
            verify(roomRepository, times(1)).getDisabled();
        }
    }

    @Nested
    class GetAllOrdered {
        @Test
        void getAllOrderedRoomsSuccessfully() {
            // Arrange
            List<Room> orderedRooms = Collections.singletonList(room);
            List<RoomDTO> expected = Collections.singletonList(roomDTO);
            when(roomRepository.getAllOrdered()).thenReturn(orderedRooms);
            when(roomMapper.convertToDtoList(orderedRooms)).thenReturn(expected);

            // Act
            List<RoomDTO> result = roomService.getAllOrdered();

            // Assert
            assertThat(result).hasSize(1).containsExactlyElementsOf(expected);
            verify(roomRepository, times(1)).getAllOrdered();
            verify(roomMapper, times(1)).convertToDtoList(orderedRooms);
        }

        @Test
        void getAllOrderedRoomsReturnsEmptyListWhenNoRoomsExist() {
            // Arrange
            when(roomRepository.getAllOrdered()).thenReturn(Collections.emptyList());
            when(roomMapper.convertToDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

            // Act
            List<RoomDTO> result = roomService.getAllOrdered();

            // Assert
            assertThat(result).isEmpty();
            verify(roomRepository, times(1)).getAllOrdered();
        }
    }
}
