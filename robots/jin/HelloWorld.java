/*
 * HelloWorld - 最简单的 Robocode 入门机器人
 * 展示了 Robocode 机器人的基本结构：run() 主循环 + onScannedRobot() 事件处理
 *
 * 行为：
 *   - 前后移动，每次移动后旋转炮管扫描敌人
 *   - 检测到敌人时开火
 */
package jin;

import robocode.HitByBulletEvent;
import robocode.Robot;
import robocode.ScannedRobotEvent;

public class HelloWorld extends Robot {

    /**
     * run: 机器人主循环
     * 执行前后往复移动，每次移动后旋转炮管360度扫描战场
     */
    public void run() {
        // 主循环：永远不会结束
        while (true) {
            // 向前移动 100 像素
            ahead(100);
            // 旋转炮管 360 度（同时扫描敌人）
            turnGunRight(360);
            // 向后移动 100 像素
            back(100);
            // 再次旋转炮管扫描
            turnGunRight(360);
        }
    }

    /**
     * onScannedRobot: 当雷达检测到敌人时调用
     * 直接向检测到的敌人发射子弹
     */
    public void onScannedRobot(ScannedRobotEvent e) {
        // 发射火力为 1 的子弹
        fire(1);
    }

    /**
     * onHitByBullet: 当被子弹击中时调用
     * 转向垂直于子弹来向，尝试躲避后续攻击
     */
    public void onHitByBullet(HitByBulletEvent e) {
        // 转向以躲避后续子弹
        turnLeft(90 - e.getBearing());
    }
}
