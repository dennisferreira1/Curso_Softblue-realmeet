package br.com.sw2you.realmeet.integration;

import static org.junit.jupiter.api.Assertions.*;

import br.com.sw2you.realmeet.api.facade.AllocationApi;
import br.com.sw2you.realmeet.core.BaseIntegrationTest;
import br.com.sw2you.realmeet.domain.repository.AllocationRepository;
import br.com.sw2you.realmeet.domain.repository.RoomRepository;
import br.com.sw2you.realmeet.email.EmailSender;
import br.com.sw2you.realmeet.utils.ConstantsTest;
import br.com.sw2you.realmeet.utils.TestDataCreator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.HttpClientErrorException;

public class AllocationApiIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private AllocationApi api;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private AllocationRepository allocationRepository;

    @MockBean
    private EmailSender emailSender;

    @Override
    protected void setupEach() throws Exception {
        setLocalHostBasePath(api.getApiClient(), "/v1");
    }

    @Test
    void testCreateAllocationSuccess() {
        var room = roomRepository.saveAndFlush(TestDataCreator.newRoomBuilderDefault().build());
        var createAllocationDTO = TestDataCreator.newCreateAllocationDTO().roomId(room.getId());
        var allocationDTO = api.createAllocation(ConstantsTest.TEST_CLIENT_API_KEY, createAllocationDTO);

        Assertions.assertNotNull(allocationDTO.getId());
        assertEquals(room.getId(), allocationDTO.getRoomId());
        assertEquals(createAllocationDTO.getSubject(), allocationDTO.getSubject());
        assertEquals(createAllocationDTO.getEmployeeName(), allocationDTO.getEmployeeName());
        assertEquals(createAllocationDTO.getEmployeeEmail(), allocationDTO.getEmployeeEmail());
        assertTrue(createAllocationDTO.getStartAt().isEqual(allocationDTO.getStartAt()));
        assertTrue(createAllocationDTO.getEndAt().isEqual(allocationDTO.getEndAt()));
    }

    @Test
    void testCreateAllocationValidationError() {
        var room = roomRepository.saveAndFlush(TestDataCreator.newRoomBuilderDefault().build());
        var allocationDTO = TestDataCreator.newCreateAllocationDTO().roomId(room.getId()).subject(null);

        assertThrows(
            HttpClientErrorException.UnprocessableEntity.class,
            () -> api.createAllocation(ConstantsTest.TEST_CLIENT_API_KEY, allocationDTO)
        );
    }

    @Test
    void testCreateAllocationWhenRoomDoesNotExist() {
        assertThrows(
            HttpClientErrorException.NotFound.class,
            () -> api.createAllocation(ConstantsTest.TEST_CLIENT_API_KEY, TestDataCreator.newCreateAllocationDTO())
        );
    }

    @Test
    void testDeleteAllocationSuccess() {
        var room = roomRepository.saveAndFlush(TestDataCreator.newRoomBuilderDefault().build());
        var allocation =
            this.allocationRepository.saveAndFlush(TestDataCreator.newAllocationBuilderDefault().room(room).build());
        api.deleteAllocation(ConstantsTest.TEST_CLIENT_API_KEY, allocation.getId());

        assertFalse(allocationRepository.findById(allocation.getId()).isPresent());
    }

    @Test
    void testDeleteAllocationWhenCannotBeDeleted() {
        var room = roomRepository.saveAndFlush(TestDataCreator.newRoomBuilderDefault().build());
        var allocation =
            this.allocationRepository.saveAndFlush(
                    TestDataCreator
                        .newAllocationBuilderDefault()
                        .room(room)
                        .startAt(ConstantsTest.DEFAULT_ALLOCATION_START_AT.minusDays(2))
                        .endAt(ConstantsTest.DEFAULT_ALLOCATION_END_AT.minusDays(2))
                        .build()
                );

        assertThrows(
            HttpClientErrorException.UnprocessableEntity.class,
            () -> api.deleteAllocation(ConstantsTest.TEST_CLIENT_API_KEY, allocation.getId())
        );
    }

    @Test
    void testDeleteAllocationDoesNotExist() {
        assertThrows(
            HttpClientErrorException.NotFound.class,
            () -> api.deleteAllocation(ConstantsTest.TEST_CLIENT_API_KEY, 1L)
        );
    }

    @Test
    void testUpdateAllocationSuccess() {
        var room = roomRepository.saveAndFlush(TestDataCreator.newRoomBuilderDefault().build());
        var createAllocationDTO = TestDataCreator.newCreateAllocationDTO().roomId(room.getId());
        var allocationDTO = api.createAllocation(ConstantsTest.TEST_CLIENT_API_KEY, createAllocationDTO);

        var updateAllocationDTO = TestDataCreator
            .newUpdateAllocationDTO()
            .subject(allocationDTO.getSubject() + "_")
            .startAt(allocationDTO.getStartAt().plusDays(1))
            .endAt(allocationDTO.getEndAt().plusDays(1));

        api.updateAllocation(ConstantsTest.TEST_CLIENT_API_KEY, allocationDTO.getId(), updateAllocationDTO);

        var updatedAllocation = allocationRepository.findById(allocationDTO.getId()).orElseThrow();

        assertEquals(updateAllocationDTO.getSubject(), updatedAllocation.getSubject());
        assertTrue(updateAllocationDTO.getStartAt().isEqual(updatedAllocation.getStartAt()));
        assertTrue(updateAllocationDTO.getEndAt().isEqual(updatedAllocation.getEndAt()));
    }

    @Test
    void testUpdateAllocationDoesNotExist() {
        assertThrows(
            HttpClientErrorException.NotFound.class,
            () -> api.updateAllocation(ConstantsTest.TEST_CLIENT_API_KEY, 1L, TestDataCreator.newUpdateAllocationDTO())
        );
    }

    @Test
    void testUpdateAllocationValidationError() {
        var room = roomRepository.saveAndFlush(TestDataCreator.newRoomBuilderDefault().build());
        var allocation = allocationRepository.saveAndFlush(
            TestDataCreator.newAllocationBuilderDefault().room(room).build()
        );

        assertThrows(
            HttpClientErrorException.UnprocessableEntity.class,
            () ->
                api.updateAllocation(
                    ConstantsTest.TEST_CLIENT_API_KEY,
                    allocation.getId(),
                    TestDataCreator.newUpdateAllocationDTO().subject(null)
                )
        );
    }
}
