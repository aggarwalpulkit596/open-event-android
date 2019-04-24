package org.fossasia.openevent.general.order

import io.reactivex.Flowable
import io.reactivex.Single
import org.fossasia.openevent.general.attendees.Attendee
import org.fossasia.openevent.general.attendees.AttendeeDao

class OrderService(
    private val orderApi: OrderApi,
    private val orderDao: OrderDao,
    private val attendeeDao: AttendeeDao
) {
    fun placeOrder(order: Order): Single<Order> {
        return orderApi.placeOrder(order)
                .map { order ->
                    val attendeeIds = order.attendees?.map { order.id }
                    if (attendeeIds != null) {
                        attendeeDao.getAttendeesWithIds(attendeeIds).map {
                            if (it.size == attendeeIds.size) {
                                orderDao.insertOrder(order)
                            }
                        }
                    }
                    order
                }
    }

    fun chargeOrder(identifier: String, charge: Charge): Single<Charge> {
        return orderApi.chargeOrder(identifier, charge)
    }

    fun confirmOrder(identifier: String, order: ConfirmOrder): Single<Order> {
        return orderApi.confirmOrder(identifier, order)
    }

    fun attendeesUnderOrder(orderIdentifier: String): Single<List<Attendee>> {
        return orderApi.attendeesUnderOrder(orderIdentifier)
    }


    fun fetchTicketsFromDb(userId: Long): Flowable<List<Order>> {
        val ticketsFlowable = orderDao.getOrdersUnderUser()
        return ticketsFlowable.switchMap {
            if (it.isNotEmpty())
                ticketsFlowable
            else
                orderApi.ordersUnderUser(userId)
                    .toFlowable()
                    .map {
                        orderDao.insertOrders(it)
                    }
                    .flatMap {
                        ticketsFlowable
                    }
        }
    }
}
