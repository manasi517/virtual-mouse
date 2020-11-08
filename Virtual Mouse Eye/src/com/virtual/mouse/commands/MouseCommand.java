/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.virtual.mouse.commands;

import java.awt.Robot;
import java.awt.event.InputEvent;

public class MouseCommand {

    public static final byte BUTTON_LEFT = 0;
    public static final byte BUTTON_RIGHT = 1;
    public static final byte BUTTON_SCROLL = 2;
    //
    public static final byte OPERATION_CURSOR_MOVE_BY = 0;
    public static final byte OPERATION_CLICK = 1;
    public static final byte OPERATION_PRESS = 2;
    public static final byte OPERATION_RELEASE = 3;
    public static final byte OPERATION_VERTICAL_SCROLL_BY = 4;
    public static final byte OPERATION_HORIZONTAL_SCROLL_BY = 5;
    //
    //
    Robot robot;
    private int button;

    public MouseCommand(Robot robot) {
        this.robot = robot;
    }

    public void assignButtonFromCode(byte buttonCode) {
        switch (buttonCode) {
            case BUTTON_LEFT:
                button = InputEvent.BUTTON1_DOWN_MASK;
                break;
            case BUTTON_RIGHT:
                button = InputEvent.BUTTON3_DOWN_MASK;
                break;
            case BUTTON_SCROLL:
                button = InputEvent.BUTTON2_DOWN_MASK;
                break;
        }
    }

    public boolean execute(int operation, int[] values) {
        switch (operation) {
            case OPERATION_CURSOR_MOVE_BY:
                moveTo(values[0], values[1]);
                break;

            case OPERATION_CLICK:
                robot.mousePress(button);
                robot.mouseRelease(button);
                System.out.println("CLICK");
                break;

            case OPERATION_PRESS:
                robot.mousePress(button);
                System.out.println("Press");
                break;

            case OPERATION_RELEASE:
                robot.mouseRelease(button);
                System.out.println("Release");
                break;

            /*case OPERATION_VERTICAL_SCROLL_BY:
                robot.mouseWheel(values[0]);
                System.out.println("Scroll Y");
                break;

            case OPERATION_HORIZONTAL_SCROLL_BY:
                robot.mouseWheel(values[0]);
                System.out.println("Scroll X");
                break;*/
        }

        return true;
    }
    
    public void move(int x,int y)
    {
        x=x*4;
        y=y*3;
//         robot.mouseMove(x, y);
    }

    private void moveTo(int x, int y) {
        robot.mouseMove(x / 3, y / 3);
        robot.mouseMove(x / 2, y / 2);
        robot.mouseMove(2 * x / 3, 2 * y / 3);
        robot.mouseMove(x, y);
    }
}
