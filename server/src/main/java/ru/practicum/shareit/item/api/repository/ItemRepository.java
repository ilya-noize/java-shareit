package ru.practicum.shareit.item.api.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.item.entity.Item;
import ru.practicum.shareit.request.entity.ItemRequest;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    @Query("select i from Item i where i.owner.id = ?1 order by i.id")
    List<Item> findAllByOwner_Id(Long id, Pageable pageable);

    @Query("select i from Item i " +
            "where ( " +
            "upper(i.name) like upper(concat('%', :search, '%')) or upper(i.description) like upper(concat('%', :search, '%')) " +
            ") and i.available = true " +
            "order by i.id")
    List<Item> searchItemByNameOrDescription(
            @Param("search") String text, Pageable pageable);

    @Query("select not(count(i) > 0) from Item i where i.id = ?1 and i.owner.id = ?2")
    boolean notExistsByIdAndOwner_Id(Long itemId, Long ownerId);

    @Transactional
    @Modifying
    @Query("delete from Item i where i.id = ?1 and i.owner.id = ?2")
    void deleteByIdAndOwner_Id(Long itemId, Long ownerId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Item i set i.name = :name, i.description = :description where i.id = :id")
    void updateNameAndDescriptionById(
            @Param("name") String name,
            @Param("description") String description,
            @Param("id") Long id);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Item i set i.name = :name, i.available = :available where i.id = :id")
    void updateNameAndAvailableById(
            @Param("name") String name,
            @Param("available") boolean available,
            @Param("id") Long id);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Item i set i.description = :description, i.available = :available where i.id = :id")
    void updateDescriptionAndAvailableById(
            @Param("description") String description,
            @Param("available") boolean available,
            @Param("id") Long id);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Item i set i.name = :name where i.id = :id")
    void updateNameById(
            @Param("name") String name,
            @Param("id") Long id);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Item i set i.description = :description where i.id = :id")
    void updateDescriptionById(
            @Param("description") String description,
            @Param("id") Long id);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Item i set i.available = :available where i.id = :id")
    void updateAvailableById(
            @Param("available") boolean available,
            @Param("id") Long id);

    @Query("select i from Item i where i.request.id = ?1")
    List<Item> getByRequest_Id(Long id);

    @Query("select i from Item i where i.request in ?1")
    List<Item> findByRequestIn(List<ItemRequest> requests);
}

