package com.cbkii.btandroidts.domain.peripheral

interface BluetoothBondController {
	suspend fun createBond(address: BluetoothAddress): BondingResult
	suspend fun removeBond(address: BluetoothAddress): BondingResult
}
