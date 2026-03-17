# Gesture-Based-Mobile-Automation-App
Quick Flip Actions is an Android mobile application that allows users to trigger customizable actions using physical phone flip gestures, without touching the screen.  By combining the Accelerometer and Gyroscope, the app detects intentional flip gestures and maps them to user-defined actions


## Project Summary

Quick Flip Actions is an Android mobile application that allows users to trigger customizable actions using physical phone flip gestures, without touching the screen.

By combining the Accelerometer and Gyroscope, the app detects intentional flip gestures and maps them to user-defined actions such as:

Taking notes

Recording audio

Capturing photos or videos

Sharing live location

Launching apps

Triggering silent mode or flashlight

The system is designed to be extensible, battery-efficient, and highly customizable, making it suitable for everyday use, accessibility, and automation.

## Problem Statement

Smartphones rely heavily on touch interaction, which is not always:

Fast

Convenient

Safe (e.g., while walking, driving, or in emergencies)

Existing gesture solutions are:

Limited in customization

Tied to specific manufacturers

Not context-aware

There is a need for a universal, customizable, gesture-driven interaction system that works across Android devices.

## Objectives
### Main Objective

To design and implement a gesture-based automation system that allows users to execute multiple actions by simply flipping their phone.

### Specific Objectives

Detect intentional flip gestures reliably

Prevent accidental triggers (pocket, bag, movement)

Allow users to assign different actions to different flips

Support multiple utility and media actions

Run efficiently in the background

Provide a clean and simple configuration UI

## Core Features
### Gesture Detection

Supported gestures:

Face-up → Face-down

Face-down → Face-up

Double tap screen to disactivate 

Detected using:

Accelerometer (gravity & orientation)

Gyroscope (rotation speed & direction)

## Customizable Actions

Users can map gestures to actions such as:

### Productivity

Voice memo recording

Start task timer

### Media

Take picture

Start/stop video recording

### Location & Safety

Share live location

Send SOS message

Save current location

### System Actions

Silent / normal mode

Flashlight on/off

Launch specific app