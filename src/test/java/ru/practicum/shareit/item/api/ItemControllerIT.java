package ru.practicum.shareit.item.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.practicum.shareit.item.api.dto.ItemDto;
import ru.practicum.shareit.item.api.dto.ItemSimpleDto;
import ru.practicum.shareit.item.comment.api.dto.CommentDtoRecord;
import ru.practicum.shareit.item.comment.api.dto.CommentSimpleDto;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.practicum.shareit.ShareItApp.HEADER_USER_ID;
import static ru.practicum.shareit.item.api.ItemController.*;

@WebMvcTest(controllers = ItemController.class)
class ItemControllerIT {
    private final ItemSimpleDto itemRequest = ItemSimpleDto.builder()
            .id(1)
            .name("Item")
            .description("Description")
            .available(true).build();
    private final ItemSimpleDto itemRequestPatch = ItemSimpleDto.builder()
            .id(1)
            .name("ItemUpdate")
            .description("DescriptionUpdate")
            .available(false).build();
    private final List<ItemSimpleDto> itemSearchResponse = List.of(itemRequest);

    private final ItemDto itemResponse = ItemDto.builder()
            .id(1)
            .name("Item")
            .description("Description")
            .available(true)
            .lastBooking(null)
            .nextBooking(null)
            .comments(List.of()).build();
    private final List<ItemDto> itemListResponse = List.of(itemResponse);

    // created at 2000 year Jan, 1, PM12:00:00.000
    private final LocalDateTime now = LocalDateTime.of(2000, 1, 1, 12, 0, 0, 0);
    private final CommentSimpleDto commentRequest = CommentSimpleDto.builder()
            .itemId(1)
            .authorId(1)
            .text("Comment")
            .created(now).build();
    private final CommentDtoRecord commentResponse =
            new CommentDtoRecord(1, "Comment", "user", now);

    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private MockMvc mvc;
    @MockBean
    private ItemService itemService;

    @Test
    void create() throws Exception {
        when(itemService.create(1, itemRequest))
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
                .create(1, itemRequest);
    }

    @Test
    void update() throws Exception {
        itemResponse.setName("ItemUpdate");
        itemResponse.setDescription("DescriptionUpdate");
        itemResponse.setAvailable(false);

        when(itemService.update(1, 1, itemRequestPatch))
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
                .update(1, 1, itemRequestPatch);
    }

    @Test
    void get_whenUserIdAndItemIdExists_thenReturnDto() throws Exception {
        when(itemService.get(1, 1))
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
    void delete() throws Exception {
        doNothing().when(itemService).delete(1, 1);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .delete(DELETE_ITEM, 1)
                .header(HEADER_USER_ID, 1);

        mvc.perform(requestBuilder)
                .andExpect(status().isOk());

        verify(itemService, times(1)).delete(1, 1);
    }

    @Test
    void search() throws Exception {
        String search = "Item";
        when(itemService.search(search))
                .thenReturn(itemSearchResponse);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get(SEARCH_ITEM)
                .header(HEADER_USER_ID, 1)
                .param("text", search)
                .characterEncoding(StandardCharsets.UTF_8);

        ResultMatcher[] resultMatchers = {
                jsonPath("$[0].id").value(itemSearchResponse.get(0).getId()),
                jsonPath("$[0].name").value(itemSearchResponse.get(0).getName()),
                jsonPath("$[0].description").value(itemSearchResponse.get(0).getDescription()),
                jsonPath("$[0].available").value(itemSearchResponse.get(0).getAvailable())
        };

        mvc.perform(requestBuilder)
                .andExpectAll(resultMatchers)
                .andExpect(status().isOk());

    }

    @Test
    void getAll() throws Exception {
        when(itemService.getAll(1))
                .thenReturn(itemListResponse);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get(GET_ALL_ITEMS)
                .header(HEADER_USER_ID, 1);

        ResultMatcher[] resultMatchers = {
                jsonPath("$[0].id").value(itemResponse.getId()),
                jsonPath("$[0].name").value(itemResponse.getName()),
                jsonPath("$[0].description").value(itemResponse.getDescription()),
                jsonPath("$[0].available").value(itemResponse.getAvailable())
        };

        mvc.perform(requestBuilder)
                .andExpectAll(resultMatchers)
                .andExpect(status().isOk());
    }

    @Test
    void createComment() throws Exception {
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

        Mockito.verify(itemService).createComment(Mockito.any(CommentSimpleDto.class));
    }
}