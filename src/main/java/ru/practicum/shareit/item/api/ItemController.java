package ru.practicum.shareit.item.api;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.api.dto.ItemDto;
import ru.practicum.shareit.item.api.dto.ItemSimpleDto;
import ru.practicum.shareit.item.comment.api.dto.CommentDtoRecord;
import ru.practicum.shareit.item.comment.api.dto.CommentSimpleDto;
import ru.practicum.shareit.valid.group.Create;
import ru.practicum.shareit.valid.group.Update;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.shareit.ShareItApp.HEADER_USER_ID;

@RestController
@RequiredArgsConstructor
public class ItemController {
    public static final String CREATE_ITEM = "/items";
    public static final String UPDATE_ITEM = "/items/{id}";
    public static final String GET_ITEM = "/items/{id}";
    public static final String DELETE_ITEM = "/items/{id}";
    public static final String SEARCH_ITEM = "/items/search";
    public static final String GET_ALL_ITEMS = "/items";
    public static final String CREATE_COMMENT = "/items/{id}/comment";
    private final ItemService service;

    @PostMapping(CREATE_ITEM)
    public ItemDto create(
            @RequestHeader(HEADER_USER_ID) Integer userId,
            @RequestBody
            @Validated(Create.class) ItemSimpleDto itemDto) {

        return service.create(userId, itemDto);
    }

    @PatchMapping(UPDATE_ITEM)
    public ItemDto update(
            @RequestHeader(HEADER_USER_ID) Integer userId,
            @PathVariable(name = "id") Integer itemId,
            @RequestBody
            @Validated(Update.class) ItemSimpleDto itemDto) {

        return service.update(userId, itemId, itemDto);
    }

    @GetMapping(GET_ITEM)
    public ItemDto get(
            @RequestHeader(HEADER_USER_ID) Integer userId,
            @PathVariable(name = "id") Integer itemId) {

        return service.get(userId, itemId);
    }

    @DeleteMapping(DELETE_ITEM)
    public void delete(
            @RequestHeader(HEADER_USER_ID) Integer userId,
            @PathVariable(name = "id") Integer itemId) {

        service.delete(userId, itemId);
    }

    @GetMapping(SEARCH_ITEM)
    public List<ItemSimpleDto> search(
            @RequestParam(name = "text") String textSearch) {

        return service.search(textSearch);
    }

    @GetMapping(GET_ALL_ITEMS)
    public List<ItemDto> getAll(
            @RequestHeader(HEADER_USER_ID) Integer userId) {

        return service.getAll(userId);
    }

    @PostMapping(CREATE_COMMENT)
    public CommentDtoRecord createComment(
            @RequestHeader(HEADER_USER_ID) Integer userId,
            @PathVariable(name = "id") Integer itemId,
            @RequestBody
            @Validated(Create.class) CommentSimpleDto commentSimpleDto) {
        commentSimpleDto.setItemId(itemId);
        commentSimpleDto.setAuthorId(userId);
        commentSimpleDto.setCreated(LocalDateTime.now());

        return service.createComment(commentSimpleDto);
    }
}
