# MyRobocode

A Robocode robot project featuring an entry-level example and an advanced combat robot.

## Project Overview

This project contains two Robocode robots:

### 1. HelloWorld
- Path: `robots/jin/HelloWorld.java`
- A simple Robocode beginner robot
- Behavior: Moves back and forth, rotates the gun to scan for enemies, fires when detected

### 2. Gengar
- Path: `robots/jin/Gengar.java`
- An advanced combat robot implementing the following core features:
  - **Anti-Gravity Movement**: Calculates repulsion forces based on enemy positions and wall proximity for automatic evasion
  - **Circular Targeting**: Predicts enemy circular motion trajectory to improve hit rate
  - **Radar Lock**: Narrow beam continuous tracking of enemies
  - **Energy Management**: Adjusts firepower based on distance and energy advantage/disadvantage
  - **Bullet Dodging**: Detects enemy energy drop to predict shots and dodge by changing direction

## Requirements

- JDK 8+
- Robocode 1.10.1+

## Usage

1. Copy the `robots/jin/` directory to the `robots/` folder in your Robocode installation
2. Add JDK path via `Options > Preferences > Development Options` in Robocode
3. Open and compile via `Robot > Editor`
4. Create a battle and select robots via `Battle > New`

## Project Structure

```
MyRobocode/
├── README.md
├── .gitignore
└── robots/
    └── jin/
        ├── HelloWorld.java    # Entry-level robot
        └── Gengar.java        # Advanced combat robot
```

## Author

JinJinJinyh

## License

This code is for educational purposes only.
