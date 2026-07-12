package io.github.runc0derun.shared.playback

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@SuppressLint("PrivateApi")
class CarRestrictionsManager(private val context: Context) {
    private val _isVideoRestricted = MutableStateFlow(false)
    val isVideoRestricted: StateFlow<Boolean> = _isVideoRestricted.asStateFlow()

    private var carInstance: Any? = null
    private var uxRestrictionsManager: Any? = null

    init {
        checkAutomotiveRestrictions()
    }

    private fun checkAutomotiveRestrictions() {
        val isAutomotive = context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
        if (!isAutomotive) return

        try {
            // Dynamically load the android.car.Car class to avoid ClassNotFoundException on standard devices
            val carClass = Class.forName("android.car.Car")
            val createCarMethod = carClass.getMethod("createCar", Context::class.java)
            val carObj = createCarMethod.invoke(null, context)
            carInstance = carObj

            // Get CAR_UX_RESTRICTION_SERVICE
            val getCarManagerMethod = carClass.getMethod("getCarManager", String::class.java)
            val carUxRestrictionServiceField = carClass.getField("CAR_UX_RESTRICTION_SERVICE")
            val carUxRestrictionServiceName = carUxRestrictionServiceField.get(null) as String
            
            val uxManagerObj = getCarManagerMethod.invoke(carObj, carUxRestrictionServiceName)
            uxRestrictionsManager = uxManagerObj

            // Register listener
            val uxListenerClass = Class.forName("android.car.drivingstate.CarUxRestrictionsManager" + '$' + "OnUxRestrictionsChangedListener")
            val registerListenerMethod = uxManagerObj.javaClass.getMethod(
                "registerListener",
                uxListenerClass
            )

            // Implement the interface using java.lang.reflect.Proxy
            val listenerProxy = java.lang.reflect.Proxy.newProxyInstance(
                context.classLoader,
                arrayOf(uxListenerClass)
            ) { _, method, args ->
                if (method.name == "onUxRestrictionsChanged") {
                    val restrictions = args[0]
                    // Call isRequiresDistractionOptimization()
                    val isRequiresDistractionOptimizationMethod = restrictions.javaClass.getMethod("isRequiresDistractionOptimization")
                    val requiresDistraction = isRequiresDistractionOptimizationMethod.invoke(restrictions) as Boolean
                    _isVideoRestricted.value = requiresDistraction
                }
                null
            }

            registerListenerMethod.invoke(uxManagerObj, listenerProxy)

            // Query initial state
            val getCurrentUxRestrictionsMethod = uxManagerObj.javaClass.getMethod("getCurrentCarUxRestrictions")
            val currentRestrictions = getCurrentUxRestrictionsMethod.invoke(uxManagerObj)
            if (currentRestrictions != null) {
                val isRequiresDistractionOptimizationMethod = currentRestrictions.javaClass.getMethod("isRequiresDistractionOptimization")
                val requiresDistraction = isRequiresDistractionOptimizationMethod.invoke(currentRestrictions) as Boolean
                _isVideoRestricted.value = requiresDistraction
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            if (uxRestrictionsManager != null) {
                val unregisterListenerMethod = uxRestrictionsManager!!.javaClass.getMethod("unregisterListener")
                unregisterListenerMethod.invoke(uxRestrictionsManager)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            val carClass = carInstance?.javaClass ?: return
            val disconnectMethod = carClass.getMethod("disconnect")
            disconnectMethod.invoke(carInstance)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
