package eu.darken.myperm.apps.ui.list.apps

import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import coil.load
import eu.darken.myperm.R
import eu.darken.myperm.apps.core.InternetAccess
import eu.darken.myperm.apps.core.types.BaseApp
import eu.darken.myperm.apps.core.types.NormalApp
import eu.darken.myperm.apps.ui.list.AppsAdapter
import eu.darken.myperm.common.debug.logging.log
import eu.darken.myperm.common.getColorForAttr
import eu.darken.myperm.common.lists.BindableVH
import eu.darken.myperm.databinding.AppsNormalItemBinding
import eu.darken.myperm.permissions.core.AndroidPermissions
import eu.darken.myperm.permissions.core.Permission

class NormalAppVH(parent: ViewGroup) : AppsAdapter.BaseVH<NormalAppVH.Item, AppsNormalItemBinding>(
    R.layout.apps_normal_item,
    parent
), BindableVH<NormalAppVH.Item, AppsNormalItemBinding> {

    override val viewBinding = lazy { AppsNormalItemBinding.bind(itemView) }

    private val defaultTagColor = context.getColorForAttr(R.attr.colorOnBackground)

    private var permissionNavListener: ((Permission) -> Unit)? = null

    override val onBindData: AppsNormalItemBinding.(
        item: Item,
        payloads: List<Any>
    ) -> Unit = { item, _ ->
        permissionNavListener = item.onShowPermission
        val app = item.app

        packageName.text = app.packageName

        label.apply {
            text = app.label
            isSelected = true
        }

        permissionInfo.apply {
            val grantedCount = app.requestedPermissions.count { it.isGranted }
            val countTotal = app.requestedPermissions.size
            text = getString(R.string.apps_permissions_x_of_x_granted, grantedCount, countTotal)

            val declaredCount = app.declaredPermissions.size
            if (declaredCount > 0) {
                append(" " + getString(R.string.apps_permissions_declares_x, declaredCount))
            }
        }

        icon.load(app.id)

        itemView.setOnClickListener { item.onClickAction(item) }

        tagInternet.apply {
            when (app.internetAccess) {
                InternetAccess.DIRECT -> {
                    setImageResource(R.drawable.ic_baseline_signal_wifi_4_bar_24)
                    tintIt(defaultTagColor)
                }
                InternetAccess.INDIRECT -> {
                    setImageResource(R.drawable.ic_baseline_signal_wifi_statusbar_connected_no_internet_4_24)
                    tintIt(getColor(R.color.status_positive_1))
                }
                InternetAccess.NONE -> {
                    setImageResource(R.drawable.ic_baseline_signal_wifi_connected_no_internet_4_24)
                    tintIt(getColor(R.color.status_positive_2))
                }
            }
            setUpInfoSnackbar(AndroidPermissions.INTERNET)
            isVisible = true
        }

        tagBoot.setupAll(app, AndroidPermissions.BOOT_COMPLETED)

        tagStorage.apply {
            val writeStorage = app.getPermission(AndroidPermissions.WRITE_EXTERNAL_STORAGE.id)
            val readStorage = app.getPermission(AndroidPermissions.READ_EXTERNAL_STORAGE.id)
            isInvisible = writeStorage == null && readStorage == null
            when {
                writeStorage?.isGranted == true -> tintIt(getColor(R.color.status_negative_1))
                readStorage?.isGranted == true -> tintIt(defaultTagColor)
            }
            setUpInfoSnackbar(AndroidPermissions.WRITE_EXTERNAL_STORAGE)
        }

        tagWakelock.setupAll(app, AndroidPermissions.WAKE_LOCK)

        tagVibrate.setupAll(app, AndroidPermissions.VIBRATE)
    }

    private fun ImageView.tintIt(@ColorInt color: Int) {
        setColorFilter(color)
    }

    private fun ImageView.setupAll(
        app: BaseApp,
        permission: Permission,
        @ColorInt colorGranted: Int = defaultTagColor,
        @ColorInt colorDenied: Int = ContextCompat.getColor(context, R.color.status_positive_1),
    ) {
        isInvisible = when (app.getPermission(permission.id)?.isGranted) {
            true -> tintIt(colorGranted).let { false }
            false -> tintIt(colorDenied).let { false }
            null -> true
        }
        setUpInfoSnackbar(permission)
    }

    private fun ImageView.setUpInfoSnackbar(permission: Permission) {
        setOnClickListener {
            log { "Permission tag clicked: $permission" }
            permissionNavListener?.invoke(permission)
        }
    }

    data class Item(
        override val app: NormalApp,
        val onClickAction: (Item) -> Unit,
        val onShowPermission: ((Permission) -> Unit),
    ) : AppsAdapter.Item
}