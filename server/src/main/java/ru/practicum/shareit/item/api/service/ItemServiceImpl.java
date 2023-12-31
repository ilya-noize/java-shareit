package ru.practicum.shareit.item.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.api.dto.BookingItemDto;
import ru.practicum.shareit.booking.api.dto.BookingMapper;
import ru.practicum.shareit.booking.api.repository.BookingRepository;
import ru.practicum.shareit.constants.Constants;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.api.dto.CommentDto;
import ru.practicum.shareit.item.api.dto.CommentMapper;
import ru.practicum.shareit.item.api.dto.CommentSimpleDto;
import ru.practicum.shareit.item.api.dto.ItemDto;
import ru.practicum.shareit.item.api.dto.ItemMapper;
import ru.practicum.shareit.item.api.dto.ItemSimpleDto;
import ru.practicum.shareit.item.api.repository.CommentRepository;
import ru.practicum.shareit.item.api.repository.ItemRepository;
import ru.practicum.shareit.item.entity.CommentEntity;
import ru.practicum.shareit.item.entity.Item;
import ru.practicum.shareit.request.entity.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.api.repository.UserRepository;
import ru.practicum.shareit.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static ru.practicum.shareit.booking.entity.enums.BookingStatus.APPROVED;
import static ru.practicum.shareit.constants.Constants.ITEM_NOT_EXISTS;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ItemServiceImpl implements ItemService {
    private final Sort sortStartAsc =
            Sort.by(Sort.Direction.ASC, "start");
    private final Sort sortStartDesc =
            Sort.by(Sort.Direction.DESC, "start");
    private final ItemRepository itemRepository;
    private final ItemRequestRepository itemRequestRepository;

    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    /**
     * Создание предмета
     * <p>
     * Все поля обязательны к заполнению!
     *
     * @param userId  Идентификатор владелец предмета
     * @param itemDto Данные нового объекта
     * @return Созданный объект, после всех проверок условий
     */
    @Override
    public ItemDto create(Long userId, ItemSimpleDto itemDto) {
        log.debug("[i] CREATE ITEM:{} by User.id:{}", itemDto, userId);
        checkingExistUserById(userId);

        Item item = ItemMapper.INSTANCE.toEntity(itemDto, userId);
        Long requestId = itemDto.getRequestId();
        if (requestId != null) {
            ItemRequest itemRequest = itemRequestRepository.findById(requestId)
                    .orElseThrow(() ->
                            new NotFoundException(
                                    format(Constants.REQUEST_NOT_EXISTS, requestId)));
            log.debug("[i] add ItemRequest.id:{}", requestId);
            item.setRequest(itemRequest);
        }

        return ItemMapper.INSTANCE.toDto(itemRepository.save(item));
    }

    /**
     * Updating an item
     * <p>
     * Restrictions: only the owner of the item can edit it! <br/>
     * <ul>Partial editing is allowed:
     *     <li>{@link ItemDto#name} Name</li>
     *     <li>{@link ItemDto#description} Description</li>
     *     <li>{@link ItemDto#available} Visibility for all users</li>
     * </ul>
     *
     * @param ownerId Идентификатор владелец предмета
     * @param itemId  Идентификатор предмета
     * @param itemDto ITEM DTO
     *                Название предмета
     *                Описание предмета
     *                Доступность предмета
     * @return itemDTO
     */
    @Override
    public ItemDto update(Long ownerId, Long itemId, ItemSimpleDto itemDto) {
        log.debug("[i] UPDATE ITEM");
        String name = itemDto.getName();
        String description = itemDto.getDescription();
        Boolean available = itemDto.getAvailable();

        checkingExistUserById(ownerId);

        Item item = itemRepository.findById(itemId)
                .orElseThrow(
                        () -> new NotFoundException(
                                format(ITEM_NOT_EXISTS, itemId)));

        boolean isNotOwner = itemRepository.notExistsByIdAndOwner_Id(itemId, ownerId);
        if (isNotOwner) {
            throw new BadRequestException("Editing an item is only allowed to the owner of that item.");
        }

        boolean notNullDescription = !(description == null || description.isEmpty());
        boolean notNullName = !(name == null || name.isBlank());

        if (notNullName && notNullDescription && available != null) {

            return ItemMapper.INSTANCE.toDto(
                    itemRepository.save(
                            ItemMapper.INSTANCE.toEntity(itemDto, ownerId)));
        }

        return ItemMapper.INSTANCE.toDto(
                partiallyUpdated(itemId, name, description, available, item));
    }

    /**
     * Receiving an item with the latest booking and comments.
     * Target:
     * {@code (/items/{id})}
     *
     * @param userId User ID
     * @param itemId Item ID
     * @return Item with/without Booking
     */
    @Override
    public ItemDto get(Long userId, Long itemId) {

        checkingExistUserById(userId);
        LocalDateTime now = LocalDateTime.now();
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() ->
                        new NotFoundException(String.format(ITEM_NOT_EXISTS, itemId)));
        ItemDto itemDto = ItemMapper.INSTANCE.toDto(item);

        BookingItemDto lastBooking = bookingRepository
                .findFirstByItem_IdAndStartLessThanEqualAndStatus(itemId, now, APPROVED, sortStartDesc)
                .stream()
                .map(BookingMapper.INSTANCE::toItemDto)
                .findFirst()
                .orElse(null);
        BookingItemDto nextBooking = bookingRepository
                .findFirstByItem_IdAndStartAfterAndStatus(itemId, now, APPROVED, sortStartAsc)
                .stream()
                .map(BookingMapper.INSTANCE::toItemDto)
                .findFirst()
                .orElse(null);
        List<CommentDto> comments = commentRepository
                .findAllByItem_IdOrderByCreatedDesc(itemId)
                .stream()
                .map(CommentMapper.INSTANCE::toDto)
                .collect(Collectors.toList());

        boolean isUserByOwnerByItem = item.getOwner().getId().equals(userId);
        if (isUserByOwnerByItem) {
            itemDto.setLastBooking(lastBooking);
            itemDto.setNextBooking(nextBooking);
        }
        itemDto.setComments(comments);

        return itemDto;
    }

    /**
     * Getting a list of items with the latest booking and comments
     * for both the owner of the items and users.
     * {@code (/items)}
     *
     * @param ownerId  User ID
     * @param pageable Постранично
     * @param now      Точное время
     * @return List of user's items
     */

    @Transactional(readOnly = true)
    @Override
    public List<ItemDto> getAll(Long ownerId, Pageable pageable, LocalDateTime now) {
        checkingExistUserById(ownerId);
        List<ItemDto> itemsDto = itemRepository.findAllByOwner_Id(ownerId, pageable)
                .stream()
                .map(ItemMapper.INSTANCE::toDto)
                .collect(Collectors.toList());

        List<Long> itemIds = itemsDto.stream()
                .map(ItemDto::getId)
                .collect(toList());

        Map<Long, BookingItemDto> lastBookingStorage = bookingRepository
                .findByItem_IdInAndStartLessThanEqualAndStatus(
                        itemIds, now, APPROVED, sortStartDesc)
                .stream()
                .map(BookingMapper.INSTANCE::toItemDto)
                .collect(toMap(BookingItemDto::getItemId,
                        Function.identity(),
                        (first, second) -> first));

        Map<Long, BookingItemDto> nextBookingStorage = bookingRepository
                .findByItem_IdInAndStartAfterAndStatus(
                        itemIds, now, APPROVED, sortStartAsc)
                .stream()
                .map(BookingMapper.INSTANCE::toItemDto)
                .collect(toMap(BookingItemDto::getItemId,
                        Function.identity(),
                        (first, second) -> first));

        Map<Long, List<CommentEntity>> commentStorage = commentRepository
                .findByItem_IdInOrderByCreatedDesc(itemIds)
                .stream()
                .filter(Objects::nonNull)
                .collect(groupingBy((comment) -> comment.getItem().getId(), toList()));

        itemsDto.forEach(itemDto -> {
            Long itemId = itemDto.getId();
            itemDto.setLastBooking(lastBookingStorage.get(itemId));
            itemDto.setNextBooking(nextBookingStorage.get(itemId));
            itemDto.setComments(getCommentDto(commentStorage.get(itemId)));
        });

        return itemsDto;
    }

    /**
     * Creating a list of comments for the backend
     *
     * @param comments comments from DB
     * @return list of comments for the backend
     */
    private List<CommentDto> getCommentDto(
            List<CommentEntity> comments) {
        if (comments == null) {

            return null;
        }

        return comments.stream()
                .map(CommentMapper.INSTANCE::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Search for an item in the repository
     * <p>
     * If the query string is empty, output an empty list
     *
     * @param searchText текст для поиска
     * @param pageable   Постранично
     * @return Список найденных вещей
     */
    @Override
    public List<ItemSimpleDto> search(String searchText, Pageable pageable) {
        log.debug("[i] SEARCH text:{}", searchText);
        if (searchText.isBlank()) {

            return List.of();
        }

        return itemRepository
                .searchItemByNameOrDescription(searchText, pageable)
                .stream()
                .map(ItemMapper.INSTANCE::toSimpleDto)
                .collect(toList());
    }

    /**
     * Adding a comment to the subject from the user,
     * who rented it at least 1 time.
     *
     * @param commentSimpleDto Comment DTO Source
     * @return Comment DTO Record
     */
    @Transactional
    @Override
    public CommentDto createComment(CommentSimpleDto commentSimpleDto) {
        String text = commentSimpleDto.getText().trim();
        if (text.isBlank()) {
            throw new BadRequestException("Text comment can't be blank");
        }
        Long authorId = commentSimpleDto.getAuthorId();
        Long itemId = commentSimpleDto.getItemId();
        commentSimpleDto.setText(text);
        log.debug("[i] CREATE COMMENT USER_ID:{}, ITEM_ID:{}, DTO:{}", authorId, itemId, commentSimpleDto);

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException(
                        format(Constants.USER_NOT_EXISTS, authorId)));
        checkingExistItemById(itemId);

        boolean notExistBooking = !bookingRepository
                .existsCompletedBookingByTheUserOfTheItem(
                        itemId, authorId, APPROVED, commentSimpleDto.getCreated());
        if (notExistBooking) {
            throw new BadRequestException(
                    format("User with ID:(%d) has never booked an item with ID:(%d)\n" +
                                    "either the user has not completed the booking yet\n" +
                                    "or the user is planning to book this item",
                            authorId, itemId));
        }
        CommentEntity comment = CommentMapper.INSTANCE.toEntity(commentSimpleDto);
        comment.setAuthor(author);

        return CommentMapper.INSTANCE.toDto(
                commentRepository.save(comment));
    }

    /**
     * Checking for existence of a user in the repository
     *
     * @param userId User ID
     */
    private void checkingExistUserById(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(
                    format(Constants.USER_NOT_EXISTS, userId));
        }
    }

    /**
     * Checking for existence of an item in the repository
     *
     * @param itemId Item ID
     */
    private void checkingExistItemById(Long itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new NotFoundException(
                    format(ITEM_NOT_EXISTS, itemId));
        }
    }

    private Item partiallyUpdated(Long itemId, String name, String description, Boolean available, Item item) {
        boolean notNullName = !(name == null || name.isBlank());
        boolean notNullDescription = !(description == null || description.isEmpty());

        if (notNullName) {
            if (notNullDescription) {
                log.debug("[i] Name = {}, Description = {};", name, description);
                itemRepository.updateNameAndDescriptionById(name, description, itemId);
                item.setName(name);
                item.setDescription(description);
            } else {
                if (available != null) {
                    log.debug("[i] Name = {}, Available = {};", name, available);
                    itemRepository.updateNameAndAvailableById(name, available, itemId);
                    item.setName(name);
                    item.setAvailable(available);
                } else {
                    log.debug("[i] Name = {};", name);
                    itemRepository.updateNameById(name, itemId);
                    item.setName(name);
                }
            }
        } else {
            if (notNullDescription) {
                if (available != null) {
                    log.debug("[i] Update Description = {}, Available = {};", description, available);
                    itemRepository.updateDescriptionAndAvailableById(description, available, itemId);
                    item.setDescription(description);
                    item.setAvailable(available);
                } else {
                    log.debug("[i] Description = {};", description);
                    itemRepository.updateDescriptionById(description, itemId);
                    item.setDescription(description);
                }
            } else {
                if (available != null) {
                    log.debug("[i] Available = {};", available);
                    itemRepository.updateAvailableById(available, itemId);
                    item.setAvailable(available);
                }
            }
        }

        return item;
    }
}
