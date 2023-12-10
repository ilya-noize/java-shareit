package ru.practicum.shareit.request.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestMapper;
import ru.practicum.shareit.request.dto.ItemRequestSimpleDto;
import ru.practicum.shareit.request.entity.ItemRequest;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.practicum.shareit.ShareItApp.*;
import static ru.practicum.shareit.request.controller.ItemRequestController.*;

@WebMvcTest(controllers = ItemRequestController.class)
@AutoConfigureWebMvc
@AutoConfigureMockMvc
class ItemRequestControllerTest {
    private final Pageable pageable = PageRequest.ofSize(Integer.parseInt(SIZE));

    private final ItemRequestSimpleDto requestSimpleDto = ItemRequestSimpleDto.builder()
            .description("Description").build();
    private final ItemRequestDto requestDto = ItemRequestDto.builder()
            .id(1)
            .description("Description")
            .created(null)
            .items(List.of()).build();
    private final int requesterId = 1;
    @MockBean
    private ItemRequestService itemRequestService;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    @DisplayName("CREATE_REQUEST:" + CREATE_REQUEST)
    void addItemRequest() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        when(itemRequestService.create(requesterId, requestSimpleDto, now))
                .thenReturn(requestDto);

        RequestBuilder requestBuilder = post(CREATE_REQUEST)
                .header(HEADER_USER_ID, requesterId)
                .content(mapper.writeValueAsString(requestSimpleDto))
                .characterEncoding(UTF_8)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.ALL_VALUE);

        mvc.perform(requestBuilder)
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET_BY_REQUESTER:" + GET_BY_REQUESTER)
    void getByRequesterId() throws Exception {
        User requester = RANDOM.nextObject(User.class);
        ItemRequest itemRequest = RANDOM.nextObject(ItemRequest.class);
        requester.setId(requesterId);
        itemRequest.setRequester(requester);
        List<ItemRequestDto> itemRequests = List.of(
                ItemRequestMapper.INSTANCE.toDto(itemRequest));
        itemRequests.get(0).setItems(null);

        when(itemRequestService.getByRequesterId(anyInt()))
                .thenReturn(itemRequests);

        itemRequestService.getByRequesterId(requesterId);

        RequestBuilder requestBuilder = get(GET_BY_REQUESTER)
                .header(HEADER_USER_ID, requesterId)
                .characterEncoding(UTF_8)
                .accept(MediaType.ALL_VALUE);

        mvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(itemRequests.get(0).getId()))
                .andExpect(jsonPath("$[0].description").value(itemRequests.get(0).getDescription()));

        verify(itemRequestService, times(2)).getByRequesterId(anyInt());
    }

    @Test
    @DisplayName("GET_ALL_REQUESTS:" + GET_ALL_REQUESTS)
    void getAll() throws Exception {
        requestDto.setItems(null);
        requestDto.setId(1);
        when(itemRequestService.getAll(1, pageable))
                .thenReturn(List.of(requestDto));

        RequestBuilder requestBuilder = get(GET_ALL_REQUESTS)
                .header(HEADER_USER_ID, 1)
                .characterEncoding(UTF_8)
                .accept(MediaType.ALL_VALUE);

        mvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id")
                        .value(requestDto.getId()))
                .andExpect(jsonPath("$[0].description")
                        .value(requestDto.getDescription()));


        verify(itemRequestService).getAll(1, pageable);
    }

    @Test
    @DisplayName("GET_REQUEST:" + GET_REQUEST)
    void getRequest() throws Exception {
        int requestId = 1;
        when(itemRequestService.get(anyInt(), anyInt())).thenReturn(requestDto);

        RequestBuilder requestBuilder = get(GET_REQUEST, requestId)
                .header(HEADER_USER_ID, requesterId)
                .characterEncoding(UTF_8)
                .accept(MediaType.ALL_VALUE);

        mvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(requestDto.getId()))
                .andExpect(jsonPath("$.description")
                        .value(requestDto.getDescription()));

        verify(itemRequestService).get(anyInt(), anyInt());
    }
}