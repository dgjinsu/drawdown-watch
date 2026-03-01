package com.example.drawdownwatch.notification.adapter.out.persistence;

import com.example.drawdownwatch.notification.application.port.out.NotificationLogRepositoryCustom;
import com.example.drawdownwatch.notification.domain.NotificationLog;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.drawdownwatch.notification.domain.QNotificationLog.notificationLog;
import static com.example.drawdownwatch.stock.domain.QStock.stock;
import static com.example.drawdownwatch.watchlist.domain.QWatchlistItem.watchlistItem;

@Repository
@RequiredArgsConstructor
public class NotificationLogRepositoryImpl implements NotificationLogRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<NotificationLog> findByUserIdWithFilters(Long userId, String status, String channelType,
                                                         LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(notificationLog.user.id.eq(userId));

        if (status != null) {
            builder.and(notificationLog.status.eq(status));
        }
        if (channelType != null) {
            builder.and(notificationLog.channelType.eq(channelType));
        }
        if (startDate != null) {
            builder.and(notificationLog.sentAt.goe(startDate));
        }
        if (endDate != null) {
            builder.and(notificationLog.sentAt.loe(endDate));
        }

        List<NotificationLog> content = queryFactory
                .selectFrom(notificationLog)
                .leftJoin(notificationLog.watchlistItem, watchlistItem).fetchJoin()
                .leftJoin(watchlistItem.stock, stock).fetchJoin()
                .where(builder)
                .orderBy(notificationLog.sentAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(notificationLog.count())
                .from(notificationLog)
                .where(builder);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}
