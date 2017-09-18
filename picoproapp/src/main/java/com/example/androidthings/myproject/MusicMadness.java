/**
 * Created by noahp on 9/15/2017.
 */

package com.example.androidthings.myproject;
import edu.berkeley.idd.utils.SerialMidi;

import android.util.Log;
import java.io.IOException;


public class MusicMadness extends SimplePicoPro {
    SerialMidi serialMidi;

    // Allowable Chords ---> Neil Young - derived from Neil Young's Heat of Gold
    public enum Chord {
        Em, C, D, G, Bm, Am
    }

    // MIDI controls
    int channel = 0;
    int velocity = 127;
    int timbre_value = 0;
    final int timbre_controller = 77;

    // store analog readings from ADC here
    float curr_alpha, prev_alpha, curr_beta, prev_beta, curr_gamma, prev_gamma, curr_zeta, prev_zeta = 0.0f;

    // store logic values for photoresistors
    boolean alpha, beta, gamma;

    // CONFIG
    float photo_threshold = 0.5f;
    float photo_tolerance = 0.2f;
    float timbre_tolerance = 0.05f;

    Chord curr_chord = null;


    public void setup() {
        // Initialize the serial port for communicating to a PC
        uartInit(UART6,115200);

        // Initialize the Analog-to-Digital converter on the HAT
        analogInit();

        // New Serial MIDI object
        serialMidi = new SerialMidi(UART6);
    }


    public void loop() {
        // read all analog channels and print to UART
        curr_alpha = analogRead(A0);
        curr_beta = analogRead(A1);
        curr_gamma = analogRead(A2);
        curr_zeta = analogRead(A3);
        println("ALPHA: " + curr_alpha + "  BETA: " + curr_beta + " GAMMA: " + curr_gamma + " ZETA: " + curr_zeta);

        // Adjust the Velocity based off of the distance in photoresistor values
        float dist = dist3(curr_alpha - prev_alpha, curr_beta - prev_beta, curr_gamma - prev_gamma);
        velocity = map(dist, 0.0f, 1.2f, 40, 127);

        // Detect a change in the photoresistor
        if(chordChange()) {
            // Photoresistor state
            alpha = (curr_alpha > photo_threshold) ? true : false;
            beta = (curr_beta > photo_threshold) ? true : false;
            gamma = (curr_gamma > photo_threshold) ? true : false;

            // Turn off the current playing chord
            if (curr_chord != null) {
                chordOff(curr_chord, velocity);
            }

            // 111 or 000 - Don't play any chord
            if ((alpha && beta && gamma) || (!alpha && !beta && !gamma)) {
                curr_chord = null;
            }
            // Turn on the appropriate chord
            else {
                // 001 - Em
                if (!alpha && !beta && gamma) curr_chord = Chord.Em;
                // 010 - C
                else if (!alpha && beta && !gamma) curr_chord = Chord.C;
                // 011 - D
                else if (!alpha && beta && gamma) curr_chord = Chord.D;
                // 100 - G
                else if (alpha && !beta && !gamma) curr_chord = Chord.G;
                // 101 - Bm
                else if (alpha && !beta && gamma) curr_chord = Chord.Bm;
                // 110 - Am
                else if (alpha && beta && !gamma) curr_chord = Chord.Am;

                // Play the current chord
                chordOn(curr_chord, velocity);
            }
        }

        // Change the Timbre
        if (Math.abs(curr_zeta - prev_zeta) > timbre_tolerance) {
            timbre_value = map(curr_zeta, 0.0f, 1.7f, 0, 127);
            adjustController(timbre_controller, timbre_value);
        }

        // Set previous values for next iteration
        prev_alpha = curr_alpha;
        prev_beta = curr_beta;
        prev_gamma = curr_gamma;
        prev_zeta = curr_zeta;

        // Set tempo.... need to figure this one out...
        delay(50);
    }

    public boolean chordChange() {
        return Math.abs(curr_alpha - prev_alpha) > photo_tolerance ||
                Math.abs(curr_beta - prev_beta) > photo_tolerance ||
                Math.abs(curr_gamma - prev_gamma) > photo_tolerance;
    }

    public float dist3(float x, float y, float z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public int map(float x, float in_min, float in_max, int out_min, int out_max) {
        int val = (int) ((x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min);
        if (val > out_max) val = out_max;
        if (val < out_min) val = out_min;

        return val;
    }

    public void adjustController(int controller, int value) {
        serialMidi.midi_controller_change(channel,controller,value);
    }

    public void chordOn(Chord chord, int vel) {
        switch(chord) {
            // Em
            case Em:
                serialMidi.midi_note_on(channel,SerialMidi.MIDI_E4,vel);
                serialMidi.midi_note_on(channel,SerialMidi.MIDI_G4,vel);
                serialMidi.midi_note_on(channel,SerialMidi.MIDI_B4,vel);
                break;
            // C
            case C:
                serialMidi.midi_note_on(channel,SerialMidi.MIDI_C4,vel);
                serialMidi.midi_note_on(channel,SerialMidi.MIDI_E4,vel);
                serialMidi.midi_note_on(channel,SerialMidi.MIDI_G4,vel);
                break;
            // D
            case D:
                serialMidi.midi_note_on(channel,SerialMidi.MIDI_D4,vel);
                serialMidi.midi_note_on(channel,SerialMidi.MIDI_F4 + 1,vel);
                serialMidi.midi_note_on(channel,SerialMidi.MIDI_A4,vel);
                break;
            // G
            case G:
                serialMidi.midi_note_on(channel,SerialMidi.MIDI_G4,vel);
                serialMidi.midi_note_on(channel,SerialMidi.MIDI_B4,vel);
                serialMidi.midi_note_on(channel,SerialMidi.MIDI_D4,vel);
                break;
            // Bm
            case Bm:
                serialMidi.midi_note_on(channel,SerialMidi.MIDI_B4,vel);
                serialMidi.midi_note_on(channel,SerialMidi.MIDI_D4,vel);
                serialMidi.midi_note_on(channel,SerialMidi.MIDI_F4 + 1,vel);
                break;
            // Am
            case Am:
                serialMidi.midi_note_on(channel,SerialMidi.MIDI_A4,vel);
                serialMidi.midi_note_on(channel,SerialMidi.MIDI_C4,vel);
                serialMidi.midi_note_on(channel,SerialMidi.MIDI_E4,vel);
                break;
        }

    }

    public void chordOff(Chord chord, int vel) {
        switch(chord) {
            // Em
            case Em:
                serialMidi.midi_note_off(channel,SerialMidi.MIDI_E4,vel);
                serialMidi.midi_note_off(channel,SerialMidi.MIDI_G4,vel);
                serialMidi.midi_note_off(channel,SerialMidi.MIDI_B4,vel);
                break;
            // C
            case C:
                serialMidi.midi_note_off(channel,SerialMidi.MIDI_C4,vel);
                serialMidi.midi_note_off(channel,SerialMidi.MIDI_E4,vel);
                serialMidi.midi_note_off(channel,SerialMidi.MIDI_G4,vel);
                break;
            // D
            case D:
                serialMidi.midi_note_off(channel,SerialMidi.MIDI_D4,vel);
                serialMidi.midi_note_off(channel,SerialMidi.MIDI_F4 + 1,vel);
                serialMidi.midi_note_off(channel,SerialMidi.MIDI_A4,vel);
                break;
            // G
            case G:
                serialMidi.midi_note_off(channel,SerialMidi.MIDI_G4,vel);
                serialMidi.midi_note_off(channel,SerialMidi.MIDI_B4,vel);
                serialMidi.midi_note_off(channel,SerialMidi.MIDI_D4,vel);
                break;
            // Bm
            case Bm:
                serialMidi.midi_note_off(channel,SerialMidi.MIDI_B4,vel);
                serialMidi.midi_note_off(channel,SerialMidi.MIDI_D4,vel);
                serialMidi.midi_note_off(channel,SerialMidi.MIDI_F4 + 1,vel);
                break;
            // Am
            case Am:
                serialMidi.midi_note_off(channel,SerialMidi.MIDI_A4,vel);
                serialMidi.midi_note_off(channel,SerialMidi.MIDI_C4,vel);
                serialMidi.midi_note_off(channel,SerialMidi.MIDI_E4,vel);
                break;
        }
    }

}
