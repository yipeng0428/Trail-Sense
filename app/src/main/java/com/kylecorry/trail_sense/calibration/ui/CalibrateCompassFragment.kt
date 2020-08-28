package com.kylecorry.trail_sense.calibration.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.calibration.infrastructure.CompassCalibrator
import com.kylecorry.trail_sense.shared.Throttle
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.*
import kotlin.math.roundToInt


class CalibrateCompassFragment : Fragment() {

    private lateinit var compass: ICompass
    private lateinit var compassCalibrator: CompassCalibrator
    private lateinit var gps: IGPS
    private lateinit var prefs: UserPreferences
    private val throttle = Throttle(20)
    private var useLegacyCompass = false
    private var gpsStarted = false

    private lateinit var compassValueTxt: TextView
    private lateinit var declinationValueTxt: TextView
    private lateinit var trueNorthSwitch: SwitchCompat
    private lateinit var autoDeclinationSwitch: SwitchCompat
    private lateinit var declinationOverrideEdit: EditText
    private lateinit var fromGpsBtn: Button
    private lateinit var legacyCompassSwitch: SwitchCompat

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sensor_compass, container, false)

        prefs = UserPreferences(requireContext())
        useLegacyCompass = prefs.navigation.useLegacyCompass
        compass = if (useLegacyCompass) LegacyCompass(requireContext()) else VectorCompass(requireContext())
        gps = GPS(requireContext())
        compassCalibrator = CompassCalibrator(requireContext())

        compassValueTxt = view.findViewById(R.id.compass_value)
        declinationValueTxt = view.findViewById(R.id.declination_value)
        trueNorthSwitch = view.findViewById(R.id.true_north)
        autoDeclinationSwitch = view.findViewById(R.id.auto_declination)
        declinationOverrideEdit = view.findViewById(R.id.declination_override)
        fromGpsBtn = view.findViewById(R.id.from_gps_btn)
        legacyCompassSwitch = view.findViewById(R.id.legacy_compass)

        trueNorthSwitch.isChecked = prefs.navigation.useTrueNorth
        autoDeclinationSwitch.isChecked = prefs.useAutoDeclination
        declinationOverrideEdit.setText(prefs.declinationOverride.toString())
        declinationOverrideEdit.isEnabled = !prefs.useAutoDeclination
        fromGpsBtn.isEnabled = !prefs.useAutoDeclination
        legacyCompassSwitch.isChecked = prefs.navigation.useLegacyCompass

        trueNorthSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.navigation.useTrueNorth = isChecked
            if (isChecked){
                startGps()
            }
        }

        autoDeclinationSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.useAutoDeclination = isChecked
            if (isChecked){
                startGps()
            }

            declinationOverrideEdit.isEnabled = !isChecked
            fromGpsBtn.isEnabled = !isChecked
        }

        fromGpsBtn.setOnClickListener { _ ->
            gps.start(this::updateManualDeclinationFromGps)
        }

        legacyCompassSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.navigation.useLegacyCompass = isChecked
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        compass.start(this::updateCompass)
        if (prefs.useAutoDeclination || prefs.navigation.useTrueNorth){
            startGps()
        }
    }

    override fun onPause() {
        super.onPause()
        compass.stop(this::updateCompass)
        stopGps()
    }

    private fun startGps(){
        if (gpsStarted){
            return
        }
        gpsStarted = true
        gps.start(this::updateGps)
    }

    private fun stopGps(){
        gpsStarted = false
        gps.stop(this::updateGps)
        gps.stop(this::updateManualDeclinationFromGps)
    }

    private fun updateManualDeclinationFromGps(): Boolean {
        declinationOverrideEdit.setText(compassCalibrator.getDeclinationAuto(gps.location, gps.altitude).toString())
        return false
    }

    private fun updateGps(): Boolean {
        updateCompass()
        return prefs.useAutoDeclination || prefs.navigation.useTrueNorth
    }

    private fun updateCompass(): Boolean {

        if (throttle.isThrottled()){
            return true
        }

        if (useLegacyCompass != prefs.navigation.useLegacyCompass){
            useLegacyCompass = prefs.navigation.useLegacyCompass
            compass.stop(this::updateCompass)
            compass = if (useLegacyCompass) LegacyCompass(requireContext()) else VectorCompass(requireContext())
            compass.start(this::updateCompass)

        }

        // TODO: Set smoothing

        val declination = if (prefs.useAutoDeclination){
            compassCalibrator.getDeclinationAuto(gps.location, gps.altitude)
        } else {
            val editTxtValue = declinationOverrideEdit.text.toString().toFloatOrNull()
            if (editTxtValue != null){
                compassCalibrator.setDeclinationManual(editTxtValue)
            }
            compassCalibrator.getDeclinationManual()
        }

        if (prefs.navigation.useTrueNorth) {
            compass.declination = declination
        } else {
            compass.declination = 0.0f
        }

        declinationValueTxt.text = getString(R.string.degree_format, declination.roundToInt())
        compassValueTxt.text = getString(R.string.degree_format, compass.bearing.value.roundToInt())
        return true
    }



}