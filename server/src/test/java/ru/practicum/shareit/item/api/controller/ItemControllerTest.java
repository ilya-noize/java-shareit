package ru.practicum.shareit.item.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.api.dto.CommentDto;
import ru.practicum.shareit.item.api.dto.CommentSimpleDto;
import ru.practicum.shareit.item.api.dto.ItemDto;
import ru.practicum.shareit.item.api.dto.ItemSimpleDto;
import ru.practicum.shareit.item.api.service.ItemServiceImpl;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.practicum.shareit.constants.Constants.CREATE_COMMENT;
import static ru.practicum.shareit.constants.Constants.CREATE_ITEM;
import static ru.practicum.shareit.constants.Constants.GET_ITEM;
import static ru.practicum.shareit.constants.Constants.HEADER_USER_ID;
import static ru.practicum.shareit.constants.Constants.UPDATE_ITEM;

@WebMvcTest(controllers = ItemController.class)
@AutoConfigureWebMvc
@AutoConfigureMockMvc
class ItemControllerTest {
    private final ItemSimpleDto itemRequest = ItemSimpleDto.builder()
            .id(1L)
            .name("Item")
            .description("Description")
            .available(true)
            .requestId(null).build();
    private final ItemSimpleDto itemRequestPatch = ItemSimpleDto.builder()
            .id(1L)
            .name("ItemUpdate")
            .description("DescriptionUpdate")
            .available(false).build();
    private final ItemDto itemResponse = ItemDto.builder()
            .id(1L)
            .name("Item")
            .description("Description")
            .available(true)
            .lastBooking(null)
            .nextBooking(null)
            .comments(List.of()).build();

    // created at 2000 year Jan, 1L, PM12:00:00.000
    private final LocalDateTime now = LocalDateTime.of(2000, 1, 1, 12, 0, 0, 0);
    private final CommentSimpleDto commentRequest = CommentSimpleDto.builder()
            .itemId(1L)
            .authorId(1L)
            .text("Comment")
            .created(now).build();
    private final CommentDto commentResponse =
            new CommentDto(1L, "Comment", "user", now);

    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private MockMvc mvc;
    @MockBean
    private ItemServiceImpl itemService;

    @Test
    @DisplayName("POST " + CREATE_ITEM + " when right Dto - return 200")
    void create_whenCreateItem_returnDto() throws Exception {
        when(itemService.create(1L, itemRequest))
                .thenReturn(itemResponse);

        RequestBuilder requestBuilder = post(CREATE_ITEM)
                .content(mapper.writeValueAsString(itemRequest))
                .header(HEADER_USER_ID, 1)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        ResultMatcher[] resultMatchers = {
                jsonPath("$.id").value(itemResponse.getId()),
                jsonPath("$.name").value(itemResponse.getName()),
                jsonPath("$.description").value(itemResponse.getDescription()),
                jsonPath("$.available").value(itemResponse.getAvailable())
        };

        mvc.perform(requestBuilder)
                .andExpectAll(resultMatchers)
                .andExpect(status().isOk());

        verify(itemService, times(1))
                .create(1L, itemRequest);
    }

    @Test
    @DisplayName("POST " + CREATE_ITEM + " when wrong Dto - return 400")
    void create_whenNullDto_return400() throws Exception {
        when(itemService.create(1L, null))
                .thenThrow(NullPointerException.class);

        RequestBuilder requestBuilder = post(CREATE_ITEM)
                .content(mapper.writeValueAsString(null))
                .header(HEADER_USER_ID, 1)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        mvc.perform(requestBuilder)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST " + CREATE_ITEM + " when not exists User/Item/ItemRequest - return 400")
    void create_whenNotFoundUser_Item_Request_return404() throws Exception {
        itemRequest.setRequestId(1L);
        when(itemService.create(1L, itemRequest))
                .thenThrow(NotFoundException.class);

        RequestBuilder requestBuilder = post(CREATE_ITEM)
                .content(mapper.writeValueAsString(itemRequest))
                .header(HEADER_USER_ID, 1)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        mvc.perform(requestBuilder)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST " + CREATE_ITEM + " when no " + HEADER_USER_ID + " - return 400")
    void create_whenNoUserIdInHandler_return400() throws Exception {
        when(itemService.create(1L, itemRequest))
                .thenThrow(NotFoundException.class);

        RequestBuilder requestBuilder = post(CREATE_ITEM)
                .content(mapper.writeValueAsString(itemRequest))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        mvc.perform(requestBuilder)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST " + CREATE_ITEM + " when OK return DTO")
    void create_whenWrongUserId_return404() throws Exception {
        Long wrongUserId = 9999L;
        when(itemService.create(wrongUserId, itemRequest))
                .thenThrow(NotFoundException.class);

        RequestBuilder requestBuilder = post(CREATE_ITEM)
                .content(mapper.writeValueAsString(itemRequest))
                .header(HEADER_USER_ID, wrongUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        mvc.perform(requestBuilder)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH " + UPDATE_ITEM + " when OK return DTO")
    void update_whenRightUpdate_returnDto200() throws Exception {
        itemResponse.setName("ItemUpdate");
        itemResponse.setDescription("DescriptionUpdate");
        itemResponse.setAvailable(false);

        when(itemService.update(1L, 1L, itemRequestPatch))
                .thenReturn(itemResponse);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .patch(UPDATE_ITEM, 1)
                .content(mapper.writeValueAsString(itemRequestPatch))
                .header(HEADER_USER_ID, 1)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        ResultMatcher[] resultMatchers = {
                jsonPath("$.id").value(itemResponse.getId()),
                jsonPath("$.name").value(itemResponse.getName()),
                jsonPath("$.description").value(itemResponse.getDescription()),
                jsonPath("$.available").value(itemResponse.getAvailable())
        };

        mvc.perform(requestBuilder)
                .andExpectAll(resultMatchers)
                .andExpect(status().isOk());

        verify(itemService, times(1))
                .update(1L, 1L, itemRequestPatch);
    }

    @Test
    @DisplayName("GET " + GET_ITEM + " when OK return DTO")
    void get_whenUserIdAndItemIdExists_thenReturnDto() throws Exception {
        when(itemService.get(1L, 1L))
                .thenReturn(itemResponse);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get(GET_ITEM, 1)
                .header(HEADER_USER_ID, 1);

        ResultMatcher[] resultMatchers = {
                jsonPath("$.id").value(itemResponse.getId()),
                jsonPath("$.name").value(itemResponse.getName()),
                jsonPath("$.description").value(itemResponse.getDescription()),
                jsonPath("$.available").value(itemResponse.getAvailable())
        };

        mvc.perform(requestBuilder)
                .andExpectAll(resultMatchers)
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST " + CREATE_COMMENT + " when OK return DTO")
    void createComment_whenRightData_returnDto200() throws Exception {
        when(itemService.createComment(Mockito.any(CommentSimpleDto.class))).thenReturn(commentResponse);

        RequestBuilder requestBuilder = post(CREATE_COMMENT, 1)
                .header(HEADER_USER_ID, 1)
                .content(mapper.writeValueAsString(commentRequest))
                .characterEncoding(StandardCharsets.UTF_8)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.ALL_VALUE);

        ResultMatcher[] resultMatchers = {
                jsonPath("$.id").value(commentResponse.getId()),
                jsonPath("$.authorName").value(commentResponse.getAuthorName()),
                jsonPath("$.text").value(commentResponse.getText())
        };
        mvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpectAll(resultMatchers);

        verify(itemService).createComment(Mockito.any(CommentSimpleDto.class));
    }
}