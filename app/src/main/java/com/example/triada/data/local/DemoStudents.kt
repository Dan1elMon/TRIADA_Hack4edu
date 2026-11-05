package com.example.triada.data.local

data class DemoStudent(val number:Int,val name:String) {
    val profileId get()="demo-student-${number.toString().padStart(2,'0')}"
    val accountId get()="student-$profileId"
    val username get()=(20000000+number).toString()
    val teacherNumber get()=(number-1)/10+1
    val teacherId get()=if(teacherNumber==1)"demo-teacher" else "demo-teacher-${teacherNumber.toString().padStart(2,'0')}"
}

data class DemoTeacher(val number:Int,val name:String) {
    val accountId get()=if(number==1)"demo-teacher" else "demo-teacher-${number.toString().padStart(2,'0')}"
    val username get()=if(number==1)"10000001" else (11000000+number).toString()
}

/** Fictional demo roster, with stable IDs so upgrades never duplicate or reset students. */
object DemoStudents {
    val teachers=listOf("María López","Carlos Mendoza","Elena Vargas").mapIndexed{i,name->DemoTeacher(i+1,name)}
    val all=listOf("Ana Torres","Luis Ramos","Sofía Vega","Mateo Ríos","Valentina Cruz",
        "Diego Flores","Camila Soto","Gabriel Luna","Lucía Castro","Nicolás Pérez",
        "Martín Salazar","Isabella Díaz","Sebastián Mora","Daniela Ruiz","Andrés León",
        "Paula Medina","Joaquín Silva","Mariana Paredes","Samuel Ortiz","Victoria Rojas",
        "Emilio Navarro","Antonella Reyes","Adrián Campos","Renata Molina","Tomás Herrera",
        "Mía Fuentes","Alejandro Soto","Emilia Ponce","Bruno Aguilar","Valeria Núñez")
        .mapIndexed{i,name->DemoStudent(i+1,name)}
    suspend fun ensureTeachers(db:LearningDatabase) {
        teachers.forEach{teacher->
            val previous=db.schoolDao().account(teacher.accountId)
            if(previous==null) db.schoolDao().addAccount(SchoolAccountEntity(teacher.accountId,teacher.name,"TEACHER",null))
            else if(teacher.number==1&&previous.name=="Docente de ejemplo")db.schoolDao().rename(teacher.accountId,teacher.name)
        }
    }
    suspend fun ensureProfiles(db:LearningDatabase):List<DemoStudent> {
        val created=mutableListOf<DemoStudent>()
        all.forEach { student -> if(db.learningDao().profile(student.profileId)==null) {
            db.learningDao().insertProfile(ProfileEntity(student.profileId,student.name,listOf("star","sun","flower")[(student.number-1)%3],System.currentTimeMillis()))
            created.add(student)
        }}
        return created
    }
}
