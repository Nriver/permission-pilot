package eu.darken.myperm.permissions.core

import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

interface Permission {
    val id: Id

    @Parcelize
    @JvmInline
    value class Id(val value: String) : Parcelable

    fun getLabel(context: Context): String? {
        val pm = context.packageManager

        return pm
            .getPermissionInfo(id.value, 0)
            .loadLabel(pm)
            .toString()
            .takeIf { it.isNotEmpty() }
    }
}