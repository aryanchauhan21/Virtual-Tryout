package com.leotarius.VirtualTryout

import com.google.ar.sceneform.ux.ArFragment

class CustomArFragment : ArFragment() {
    override fun getAdditionalPermissions(): Array<String> {
        val additionalPermissions = super.getAdditionalPermissions()
        val permissionLength = additionalPermissions.size
        val newPermissions = Array(permissionLength+1) { android.Manifest.permission.WRITE_EXTERNAL_STORAGE }
        if(permissionLength>0){
            System.arraycopy(additionalPermissions, 0, newPermissions, 1, permissionLength )
        }
        return newPermissions
    }
}