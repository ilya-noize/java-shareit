package ru.practicum.shareit.booking.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.api.dto.BookingDto;
import ru.practicum.shareit.booking.api.dto.BookingDtoRecord;
import ru.practicum.shareit.booking.api.dto.BookingMapper;
import ru.practicum.shareit.booking.api.repository.BookingRepository;
import ru.practicum.shareit.booking.entity.Booking;
import ru.practicum.shareit.booking.entity.enums.BookingFilterByTemplate;
import ru.practicum.shareit.exception.*;
import ru.practicum.shareit.item.api.repository.ItemRepository;
import ru.practicum.shareit.item.entity.Item;
import ru.practicum.shareit.user.api.repository.UserRepository;
import ru.practicum.shareit.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static ru.practicum.shareit.booking.entity.enums.BookingStatus.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final BookingMapper mapper;

    @Override
    public BookingDtoRecord create(Integer bookerId, BookingDto dto) {
        Integer itemId = dto.getItemId();
        Item item = itemRepository.findById(itemId)
                .orElseThrow(
                        () -> new NotFoundException(
                                format("Item with id:%s not found.",
                                        itemId)));

        if (!item.isAvailable()) {
            throw new BadRequestException("It is impossible to rent "
                    + "an item to which access is closed.");
        }

        User booker = userRepository.findById(bookerId)
                .orElseThrow(
                        () -> new NotFoundException(
                                format("User with id:%s not found.",
                                        bookerId)));

        boolean bookerIsOwnerTheItem = bookerId.equals(item.getOwner().getId());

        if (bookerIsOwnerTheItem) {
            throw new BookingException("Access denied."
                    + " You are owner this item");
        }

        LocalDateTime start = dto.getStart();
        LocalDateTime end = dto.getEnd();


        if (start.equals(end)) {
            String error = "The effective date of the lease agreement"
                    + " coincides with its termination";
            throw new RentalPeriodException(error);
        }

        if (end.isBefore(start)) {
            String error = "The effective date of the lease agreement"
                    + " after its termination";
            throw new RentalPeriodException(error);
        }

        Booking booking = mapper.toEntity(dto, bookerId);
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(WAITING);

        return mapper.toDtoRecord(
                bookingRepository.save(booking));
    }

    /**
     * Подтверждение или отклонение запроса на бронирование.<br/>
     * Может быть выполнено только владельцем вещи.<br/>
     * Затем статус бронирования становится либо APPROVED, либо REJECTED.
     *
     * @param ownerId   user ID - Owner
     * @param bookingId booking ID
     * @param approved  Booking status (true = APPROVED / false = REJECTED)
     * @return Бронирование с новым статусом
     */
    @Override
    public BookingDtoRecord update(Integer ownerId, Long bookingId, Boolean approved) {
        Booking booking;
        boolean isNotWaitingStatus;
        boolean isNotExistUser;
        Integer bookerId;
        boolean ownerIsBooker;

        booking = bookingRepository.findById(bookingId)
                .orElseThrow(
                        () -> new NotFoundException(
                                format("Booking with ID: %d not found",
                                        bookingId)));

        isNotWaitingStatus = !WAITING.equals(booking.getStatus());
        if (isNotWaitingStatus) {
            throw new BadRequestException("The booking status has already been set.");
        }

        isNotExistUser = !userRepository.existsById(ownerId);
        if (isNotExistUser) {
            throw new NotFoundException(
                    format("User with ID: %d not found",
                            ownerId));
        }

        bookerId = booking.getBooker().getId();
        ownerIsBooker = ownerId.equals(bookerId);
        if (ownerIsBooker) {
            throw new BookingException("Access denied.\n"
                    + "You cannot be the owner and the booker of this item at the same time.");
        }

        booking.setStatus(approved ? APPROVED : REJECTED);
        bookingRepository
                .updateStatusById(
                        booking.getStatus(),
                        bookingId);

        return mapper.toDtoRecord(booking);
    }

    /**
     * @param userId    User ID
     * @param bookingId Booking ID
     * @return Booking
     */
    @Override
    public BookingDtoRecord get(Integer userId, Long bookingId) {
        Booking booking;
        boolean isNotExistUser;
        Integer bookerId;
        Integer ownerId;
        boolean isBooker;
        boolean isOwner;

        booking = bookingRepository.findById(bookingId)
                .orElseThrow(
                        () -> new NotFoundException(
                                format("Booking with ID: %d not found.",
                                        bookingId)));

        isNotExistUser = !userRepository.existsById(userId);
        if (isNotExistUser) {
            throw new NotFoundException(
                    format("User with ID: %d not found",
                            userId));
        }

        bookerId = booking.getBooker().getId();
        ownerId = booking.getItem().getOwner().getId();

        isBooker = userId.equals(bookerId);
        isOwner = userId.equals(ownerId);

        if (!isBooker && !isOwner) {
            throw new BookingException("Access denied.\n"
                    + "You a not the booker/owner of the item");
        }

        return mapper.toDtoRecord(booking);
    }

    /**
     * Список предметов, взятых пользователем.
     * <ul>
     *     Фильтр поиска:
     *     <li>ALL - все записи</li>
     *     <li>CURRENT - текущие предметы в аренде (start_date after NOW + end_date before NOW)</li>
     *     <li>PAST - прошлые бронирования (end_date before NOW)</li>
     *     <li>FUTURE - будущие бронирования (start_date after NOW)</li>
     *     <li>WAITING - бронирования в ожидании решения от владельца предмета (status ==)</li>
     *     <li>REJECTED - отказы в аренде от владельца предмета (status ==)</li>
     * </ul>
     *
     * @param bookerId user ID
     * @param stateIn  Фильтр поиска
     * @return Список бронирования
     */
    @Override
    public List<BookingDtoRecord> getAllByUser(Integer bookerId, String stateIn) {
        List<Booking> bookingList;
        LocalDateTime now = LocalDateTime.now();

        BookingFilterByTemplate state =
                checkingInputParametersAndReturnEnumBookingFilterByTemplate(bookerId, stateIn);

        switch (state) {
            case CURRENT:
                bookingList = bookingRepository
                        .findAllByBooker_IdAndStartBeforeAndEndAfterOrderByStartDesc(bookerId,
                                now, now);
                break;
            case PAST:
                bookingList = bookingRepository.findAllByBooker_IdAndEndBeforeOrderByStartDesc(
                        bookerId, now);
                break;
            case ALL:
                bookingList = bookingRepository.findAllByBooker_IdOrderByStartDesc(
                        bookerId);
                break;
            case FUTURE:
                bookingList = bookingRepository.findAllByBooker_IdAndStartAfterOrderByStartDesc(
                        bookerId, now);
                break;
            case WAITING:
                bookingList = bookingRepository.findAllByBooker_IdAndStatusOrderByStartDesc(
                        bookerId, WAITING);
                break;
            case REJECTED:
                bookingList = bookingRepository.findAllByBooker_IdAndStatusOrderByStartDesc(
                        bookerId, REJECTED);
                break;
            default:
                bookingList = List.of();
        }

        return getListBookingDtoRecord(bookingList);
    }

    private BookingFilterByTemplate checkingInputParametersAndReturnEnumBookingFilterByTemplate(
            Integer userId, String stateIn) {

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(format("User with id:%s not found.", userId));
        }
        try {
            return Enum.valueOf(BookingFilterByTemplate.class, stateIn);
        } catch (Exception e) {
            throw new StateException(stateIn);
        }
    }

    /**
     * Список предметов, доступных в аренду пользователем.
     * <ul>
     *     Фильтр поиска:
     *     <li>ALL - все записи</li>
     *     <li>CURRENT - текущие предметы в аренде (start_date after NOW + end_date before NOW)</li>
     *     <li>PAST - прошлые бронирования (end_date before NOW)</li>
     *     <li>FUTURE - будущие бронирования (start_date after NOW)</li>
     *     <li>WAITING - бронирования в ожидании принятия решения (status ==)</li>
     *     <li>REJECTED - отказы в аренде от владельца предмета (status ==)</li>
     * </ul>
     *
     * @param ownerId user ID
     * @param stateIn Фильтр поиска
     * @return Список бронирования
     */
    @Override
    public List<BookingDtoRecord> getAllByOwner(Integer ownerId, String stateIn) {
        List<Booking> bookingList;
        LocalDateTime now = LocalDateTime.now();

        BookingFilterByTemplate state =
                checkingInputParametersAndReturnEnumBookingFilterByTemplate(ownerId, stateIn);

        switch (state) {
            case CURRENT:
                bookingList = bookingRepository.findAllByItem_Owner_IdAndStartBeforeAndEndAfter(ownerId,
                        now, now, Sort.by(Sort.Direction.DESC, "start"));

                break;
            case PAST:
                bookingList = bookingRepository.findAllByItem_Owner_IdAndEndBeforeOrderByStartDesc(
                        ownerId, now);
                break;
            case ALL:
                bookingList = bookingRepository
                        .findAllByItem_Owner_IdOrderByStartDesc(
                                ownerId);
                break;
            case FUTURE:
                bookingList = bookingRepository
                        .findAllByItem_Owner_IdAndStartAfterOrderByStartDesc(
                                ownerId, now);
                break;
            case WAITING:
                bookingList = bookingRepository
                        .findAllByItem_Owner_IdAndStatusOrderByStartDesc(
                                ownerId, WAITING);
                break;
            case REJECTED:
                bookingList = bookingRepository
                        .findAllByItem_Owner_IdAndStatusOrderByStartDesc(
                                ownerId, REJECTED);
                break;
            default:
                bookingList = List.of();
        }

        return getListBookingDtoRecord(bookingList);
    }

    private List<BookingDtoRecord> getListBookingDtoRecord(List<Booking> bookingList) {

        return bookingList.stream()
                .map(mapper::toDtoRecord)
                .collect(toList());
    }
}
