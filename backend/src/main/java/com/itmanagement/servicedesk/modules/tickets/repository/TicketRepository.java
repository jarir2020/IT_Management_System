package com.itmanagement.servicedesk.modules.tickets.repository;

import com.itmanagement.servicedesk.modules.tickets.model.Ticket;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    @EntityGraph(attributePaths = {"requester", "requester.department", "assignee", "assignee.department"})
    @Query("select t from Ticket t where t.requester.id = :requesterId order by t.updatedAt desc")
    List<Ticket> findAllForRequester(@Param("requesterId") Long requesterId);

    @EntityGraph(attributePaths = {"requester", "requester.department", "assignee", "assignee.department"})
    @Query("select t from Ticket t order by t.updatedAt desc")
    List<Ticket> findAllDetailed();

    @EntityGraph(attributePaths = {"requester", "requester.department", "assignee", "assignee.department"})
    @Query("select t from Ticket t where t.id = :id")
    Optional<Ticket> findDetailedById(@Param("id") Long id);
}
