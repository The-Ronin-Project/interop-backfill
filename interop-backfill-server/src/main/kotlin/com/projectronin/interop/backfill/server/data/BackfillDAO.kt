package com.projectronin.interop.backfill.server.data

import com.projectronin.interop.backfill.server.data.binding.BackfillDOs
import com.projectronin.interop.backfill.server.data.model.BackfillDO
import com.projectronin.interop.backfill.server.generated.models.Order
import com.projectronin.interop.common.ktorm.dao.BaseInteropDAO
import org.ktorm.database.Database
import org.ktorm.dsl.and
import org.ktorm.dsl.asc
import org.ktorm.dsl.desc
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.greater
import org.ktorm.dsl.insert
import org.ktorm.dsl.less
import org.ktorm.dsl.limit
import org.ktorm.dsl.map
import org.ktorm.dsl.or
import org.ktorm.dsl.orderBy
import org.ktorm.dsl.selectDistinct
import org.ktorm.dsl.update
import org.ktorm.dsl.where
import org.ktorm.schema.ColumnDeclaring
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class BackfillDAO(database: Database) : BaseInteropDAO<BackfillDO, UUID>(database) {
    override val primaryKeyColumn = BackfillDOs.id

    fun getByTenant(
        tenantId: String,
        order: Order,
        limit: Int,
        after: UUID?,
    ): List<BackfillDO> {
        logger.info { "Retrieving $limit backfills in $order order for tenant $tenantId after $after" }
        val afterBackfill =
            after?.let {
                this.getByID(it) ?: throw IllegalArgumentException("No backfill found with ID $it")
            }
        val orderBy =
            when (order) {
                Order.ASC -> listOf(BackfillDOs.startDate.asc(), BackfillDOs.id.asc())
                Order.DESC -> listOf(BackfillDOs.startDate.desc(), BackfillDOs.startDate.desc())
            }
        val query =
            database.from(BackfillDOs)
                .selectDistinct(BackfillDOs.columns)
                .where {
                    val conditions = mutableListOf<ColumnDeclaring<Boolean>>()

                    // add the search condition for tenantId
                    conditions.add(BackfillDOs.tenantId eq tenantId)

                    // add the offset clause
                    afterBackfill?.let {
                        val offset =
                            when (order) {
                                Order.ASC -> (
                                    (BackfillDOs.startDate greater it.startDate) or
                                        ((BackfillDOs.startDate eq it.startDate) and (BackfillDOs.id greater it.backfillId))
                                )

                                Order.DESC -> (
                                    (BackfillDOs.startDate less it.startDate) or
                                        ((BackfillDOs.startDate eq it.startDate) and (BackfillDOs.id less it.backfillId))
                                )
                            }
                        conditions.add(offset)
                    }

                    conditions.reduce { a, b -> a and b }
                }
                .limit(limit)
                .orderBy(orderBy)
        val backfills = query.map { BackfillDOs.createEntity(it) }
        logger.info { "Found ${backfills.size} backfills" }
        return backfills
    }

    fun insert(backfillDO: BackfillDO): UUID {
        val newUUID = UUID.randomUUID()
        logger.info { "Inserting backfill for organization ${backfillDO.tenantId} with UUID $newUUID" }
        database.insert(BackfillDOs) {
            set(it.id, newUUID)
            set(it.tenantId, backfillDO.tenantId)
            set(it.startDate, backfillDO.startDate)
            set(it.endDate, backfillDO.endDate)
            set(it.allowedResources, backfillDO.allowedResources)
        }

        logger.info { "Backfill $newUUID inserted" }
        return newUUID
    }

    fun delete(backfillId: UUID) {
        database.update(BackfillDOs) {
            set(it.isDeleted, true)
            where { it.id eq backfillId }
        }
    }
}
