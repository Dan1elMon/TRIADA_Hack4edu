package com.example.triada.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object CatalogMigration {
    val FROM_2_TO_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE questions ADD COLUMN activityType TEXT NOT NULL DEFAULT 'COUNT_OBJECTS'")
            db.execSQL("ALTER TABLE questions ADD COLUMN payloadJson TEXT NOT NULL DEFAULT '{}'")
            db.execSQL("ALTER TABLE questions ADD COLUMN answerKeyJson TEXT NOT NULL DEFAULT '{}'")
            db.execSQL("ALTER TABLE answers ADD COLUMN answerPayloadJson TEXT NOT NULL DEFAULT ''")
        }
    }
    val FROM_1_TO_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `catalog_metadata` (`id` TEXT NOT NULL, `schemaVersion` INTEGER NOT NULL, `catalogVersion` INTEGER NOT NULL, `checksum` TEXT NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `grades` (`id` TEXT NOT NULL, `number` INTEGER NOT NULL, `label` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_grades_number` ON `grades` (`number`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `subjects` (`id` TEXT NOT NULL, `gradeId` TEXT NOT NULL, `code` TEXT NOT NULL, `title` TEXT NOT NULL, `iconKey` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `referenceCount` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`gradeId`) REFERENCES `grades`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_subjects_gradeId_code` ON `subjects` (`gradeId`, `code`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_subjects_gradeId_sortOrder` ON `subjects` (`gradeId`, `sortOrder`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `learning_units` (`id` TEXT NOT NULL, `subjectId` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_learning_units_subjectId_sortOrder` ON `learning_units` (`subjectId`, `sortOrder`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `topics` (`id` TEXT NOT NULL, `subjectId` TEXT NOT NULL, `unitId` TEXT, `title` TEXT NOT NULL, `sourceOrder` INTEGER NOT NULL, `contentStatus` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(`unitId`) REFERENCES `learning_units`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_topics_subjectId_sourceOrder` ON `topics` (`subjectId`, `sourceOrder`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_topics_unitId` ON `topics` (`unitId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `catalog_lessons` (`lessonId` TEXT NOT NULL, `contentVersion` INTEGER NOT NULL, `topicId` TEXT NOT NULL, PRIMARY KEY(`lessonId`, `contentVersion`), FOREIGN KEY(`lessonId`, `contentVersion`) REFERENCES `lessons`(`id`, `version`) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(`topicId`) REFERENCES `topics`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_catalog_lessons_topicId` ON `catalog_lessons` (`topicId`)")
        }
    }
}
