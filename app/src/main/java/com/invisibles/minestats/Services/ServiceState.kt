package com.invisibles.minestats.Services

import android.content.Context
import android.content.SharedPreferences

const val name = "SERVICE_STATE"

enum class ServiceStateID {
    STARTED,
    STOPPED,
}

enum class ServicesName{
    PAYOUT_NOTIFY_SERVICE,
}

class ServiceState{

    companion object{

        fun setServiceState(context: Context, state: ServiceStateID, serviceName: ServicesName){
            val sharedPref = getSharedPref(context)
            sharedPref.edit().let {
                it.putString(serviceName.name, state.name)
                it.apply()
            }

        }

        fun getServiceState(context: Context, serviceName: ServicesName): ServiceStateID {
            val sharedPref = getSharedPref(context)
            val value = sharedPref.getString(serviceName.name, ServiceStateID.STOPPED.name).toString()
            return ServiceStateID.valueOf(value)
        }

        private fun getSharedPref(context: Context): SharedPreferences {
            return context.getSharedPreferences(name, 0)
        }

    }

}