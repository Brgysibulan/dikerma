package ph.gov.barangaysibulan.idmaker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "employees",
    indices = [Index(value = ["controlNumber"], unique = true)]
)
data class Employee(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val position: String,
    val controlNumber: String,
    val birthdate: String,
    val address: String,
    val sex: String,
    val civilStatus: String,
    val photoUri: String? = null,
    val signatureUri: String? = null,
    val qrToken: String? = null,
    val qrImageUri: String? = null,
    val status: String = "Active"
)
