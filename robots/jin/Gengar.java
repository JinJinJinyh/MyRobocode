/*
 * Gengar - 高级 Robocode 战斗机器人
 *
 * 【核心功能】综合战斗策略设计，包含以下子模块：
 *   1. 反重力移动 (Anti-Gravity Movement) - 根据敌人位置计算排斥力，配合墙壁回避
 *   2. 圆形瞄准 (Circular Targeting)   - 预测敌人圆弧运动轨迹，提高命中率
 *   3. 雷达锁定 (Radar Lock)           - 窄波束持续跟踪敌人，获取最新位置信息
 *   4. 能量管理 (Energy Management)    - 根据距离和能量优劣势调整火力
 *   5. 子弹躲避 (Bullet Dodging)       - 检测敌人能量下降，预判射击并变向闪避
 *
 * 能够稳定击败所有 Robocode 官方示例机器人 (Walls, SpinBot, Crazy, Tracker 等)
 */

package jin;

import robocode.*;
import robocode.util.Utils;

import java.awt.*;
import java.awt.geom.Point2D;

public class Gengar extends AdvancedRobot {

    // ==================== 敌人信息 ====================
    private double enemyBearing;
    private double enemyDistance;
    private double enemyHeading;
    private double enemyVelocity;
    private double enemyX;
    private double enemyY;
    private double oldEnemyHeading;
    private double enemyEnergy = 100.0;

    // ==================== 战场参数 ====================
    private double battleFieldWidth;
    private double battleFieldHeight;

    // ==================== 移动控制 ====================
    private int moveDirection = 1;

    // ==================== 主循环 ====================

    public void run() {
        // 独立控制：雷达、炮管、车身各自由事件/主循环控制
        setAdjustRadarForGunTurn(true);
        setAdjustGunForRobotTurn(true);

        // 外观 - 耿鬼配色（紫色系）
        setBodyColor(new Color(0x6B, 0x2F, 0xA0));
        setGunColor(new Color(0x4A, 0x1A, 0x7A));
        setRadarColor(new Color(0xFF, 0x00, 0x00));
        setBulletColor(new Color(0xFF, 0xD7, 0x00));
        setScanColor(new Color(0xFF, 0x00, 0x00));

        battleFieldWidth = getBattleFieldWidth();
        battleFieldHeight = getBattleFieldHeight();

        // 开局旋转雷达寻找敌人
        setTurnRadarRight(Double.POSITIVE_INFINITY);

        while (true) {
            // 如果已经找到了敌人，执行反重力移动
            if (enemyDistance > 0) {
                antiGravityMove();
            }
            execute();
        }
    }

    // ==================== 事件处理 ====================

    /**
     * 扫描到敌人时更新信息，执行瞄准/射击和雷达锁定
     */
    public void onScannedRobot(ScannedRobotEvent e) {
        // 保存扫描数据
        enemyBearing = e.getBearingRadians();
        enemyDistance = e.getDistance();
        enemyHeading = e.getHeadingRadians();
        enemyVelocity = e.getVelocity();

        // 计算敌人绝对坐标
        double absoluteBearing = getHeadingRadians() + enemyBearing;
        enemyX = getX() + enemyDistance * Math.sin(absoluteBearing);
        enemyY = getY() + enemyDistance * Math.cos(absoluteBearing);

        // 检测敌人是否开火（能量下降 0.1 ~ 3.0 视为发射子弹）
        double energyDrop = enemyEnergy - e.getEnergy();
        enemyEnergy = e.getEnergy();
        if (energyDrop > 0.1 && energyDrop <= 3.0) {
            dodgeBullet();
        }

        // 炮管瞄准 + 射击
        circularAim(absoluteBearing);

        // 雷达锁定：窄波束跟踪（雷达左右微摆保持锁定）
        double radarTurn = getHeadingRadians() + enemyBearing - getRadarHeadingRadians();
        setTurnRadarRightRadians(2.0 * Utils.normalRelativeAngle(radarTurn));
    }

    /**
     * 撞到敌人时反向移动
     */
    public void onHitRobot(HitRobotEvent e) {
        moveDirection *= -1;
        setBack(100);
    }

    /**
     * 撞墙时反向
     */
    public void onHitWall(HitWallEvent e) {
        moveDirection *= -1;
    }

    /**
     * 被子弹击中时记录（反重力移动本身已有规避能力）
     */
    public void onHitByBullet(HitByBulletEvent e) {
        // 调整移动方向降低被连续击中的概率
        moveDirection *= -1;
        setAhead(80 * moveDirection);
    }

    // ==================== 瞄准系统：圆形瞄准 ====================

    /**
     * 圆形瞄准 (Circular Targeting)
     *
     * 【算法原理】
     *   假设敌人沿圆弧轨迹运动（保持当前速度和转向速率不变），
     *   通过迭代模拟敌人未来位置，找到子弹与敌人同时到达的点。
     *
     * 【步骤】
     *   1. 根据火力计算子弹速度: bulletSpeed = 20 - 3 * bulletPower
     *   2. 每 tick 更新敌人位置: x += sin(heading) * velocity
     *                          y += cos(heading) * velocity
     *   3. 每 tick 更新敌人朝向: heading += headingChange（形成圆弧）
     *   4. 子弹可飞行距离 >= 敌人距离时停止迭代
     *   5. 计算枪口指向预测位置的角度并开火
     */
    private void circularAim(double absoluteBearing) {
        // 炮管冷却中，不射击
        if (getGunHeat() > 0.0) return;

        // === 能量管理：根据距离和自身能量决定火力 ===
        double bulletPower;
        double myEnergy = getEnergy();

        if (enemyDistance < 150) {
            bulletPower = 3.0;                          // 近距离最大化伤害
        } else if (enemyDistance < 300) {
            bulletPower = Math.min(2.0, myEnergy / 5);  // 中距离适度
        } else if (enemyDistance < 500) {
            bulletPower = Math.min(1.5, myEnergy / 8);  // 较远距离节能
        } else {
            bulletPower = Math.min(0.5, myEnergy / 12);  // 远距离只做试探射击
        }
        bulletPower = Math.max(0.1, bulletPower);

        // === 圆形预测 ===
        double bulletSpeed = 20.0 - 3.0 * bulletPower;
        double predictedX = enemyX;
        double predictedY = enemyY;
        double predictedHeading = enemyHeading;
        double deltaTime = 0;

        // 转向速率 = 当前帧朝向 - 上一帧朝向
        double headingChange = enemyHeading - oldEnemyHeading;
        oldEnemyHeading = enemyHeading;

        // 迭代预测，直到子弹到达
        while (++deltaTime * bulletSpeed <
               Point2D.Double.distance(getX(), getY(), predictedX, predictedY)) {

            predictedX += Math.sin(predictedHeading) * enemyVelocity;
            predictedY += Math.cos(predictedHeading) * enemyVelocity;
            predictedHeading += headingChange;   // 累积转向 → 形成圆弧

            // 墙壁边界钳位
            double margin = 18.0;
            if (predictedX < margin || predictedX > battleFieldWidth - margin ||
                predictedY < margin || predictedY > battleFieldHeight - margin) {
                predictedX = Math.min(Math.max(margin, predictedX),
                                      battleFieldWidth - margin);
                predictedY = Math.min(Math.max(margin, predictedY),
                                      battleFieldHeight - margin);
                break;
            }
        }

        // 计算炮管指向角度
        double gunAngle = Utils.normalAbsoluteAngle(
                Math.atan2(predictedX - getX(), predictedY - getY()));

        setTurnGunRightRadians(
                Utils.normalRelativeAngle(gunAngle - getGunHeadingRadians()));

        // 开火！
        if (Math.abs(Utils.normalRelativeAngle(gunAngle - getGunHeadingRadians())) < 0.05) {
            setFire(bulletPower);
        }
    }

    // ==================== 移动系统：反重力移动 ====================

    /**
     * 反重力移动 (Anti-Gravity Movement)
     *
     * 【算法原理】
     *   将敌人和墙壁视为施力源，对机器人施加"排斥力"。
     *   力的大小遵循平方反比定律: F = strength / distance²
     *   最终机器人沿合力方向移动，自动远离敌人和墙壁。
     *
     * 【改进】
     *   在排斥力的基础上加入"轨道力"——使机器人围绕敌人做圆周运动，
     *   而非单纯远离。这样能在保持安全距离的同时持续开火。
     */
    private void antiGravityMove() {
        double myX = getX();
        double myY = getY();

        // ---- 1. 墙壁排斥力 ----
        double WALL_MARGIN = 140;
        double WALL_STRENGTH = 180;
        double wallForceX = 0;
        double wallForceY = 0;

        // 左墙
        if (myX < WALL_MARGIN)
            wallForceX += WALL_STRENGTH * (WALL_MARGIN - myX) / WALL_MARGIN;
        // 右墙
        if (myX > battleFieldWidth - WALL_MARGIN)
            wallForceX -= WALL_STRENGTH * (myX - (battleFieldWidth - WALL_MARGIN)) / WALL_MARGIN;
        // 下墙
        if (myY < WALL_MARGIN)
            wallForceY += WALL_STRENGTH * (WALL_MARGIN - myY) / WALL_MARGIN;
        // 上墙
        if (myY > battleFieldHeight - WALL_MARGIN)
            wallForceY -= WALL_STRENGTH * (myY - (battleFieldHeight - WALL_MARGIN)) / WALL_MARGIN;

        // ---- 2. 敌人排斥力（平方反比） ----
        double enemyForceX = 0;
        double enemyForceY = 0;
        double toX = myX - enemyX;
        double toY = myY - enemyY;
        double dist = Math.sqrt(toX * toX + toY * toY);

        if (dist > 1) {
            double strength = 80000.0 / (dist * dist);   // 平方反比
            enemyForceX = strength * (toX / dist);
            enemyForceY = strength * (toY / dist);
        }

        // ---- 3. 轨道保持力（保持在约 250px 的理想战斗距离） ----
        double orbitDist = 250.0;
        double orbitForceX = 0;
        double orbitForceY = 0;
        double toEnemyX = enemyX - myX;
        double toEnemyY = enemyY - myY;
        double toEnemyDist = Math.sqrt(toEnemyX * toEnemyX + toEnemyY * toEnemyY);

        if (toEnemyDist > 1) {
            double distError = toEnemyDist - orbitDist;
            double orbitStrength = distError * 0.4;   // 距离误差缩放
            orbitForceX = orbitStrength * (toEnemyX / toEnemyDist);
            orbitForceY = orbitStrength * (toEnemyY / toEnemyDist);
        }

        // ---- 4. 切向力（绕敌人做圆周运动） ----
        double tangentForceX = 0;
        double tangentForceY = 0;
        if (toEnemyDist > 1) {
            // 垂直向量（顺时针或逆时针由 moveDirection 控制）
            double perpX = -toEnemyY / toEnemyDist;
            double perpY = toEnemyX / toEnemyDist;
            tangentForceX = moveDirection * perpX * 120;
            tangentForceY = moveDirection * perpY * 120;
        }

        // ---- 5. 合力计算 ----
        double totalX = wallForceX + enemyForceX + orbitForceX + tangentForceX;
        double totalY = wallForceY + enemyForceY + orbitForceY + tangentForceY;

        // ---- 6. 合力 → 方向角 → 移动指令 ----
        double desiredHeading = Math.atan2(totalX, totalY);
        double turnAngle = Utils.normalRelativeAngle(desiredHeading - getHeadingRadians());

        setTurnRightRadians(turnAngle);

        // 大角度转弯时降速，小角度全速
        double speed = 200 - Math.abs(turnAngle) * 60;
        setAhead(Math.max(20, Math.min(200, speed)));
    }

    // ==================== 闪避系统 ====================

    /**
     * 检测到敌人开火时改变移动方向
     * 近距离时（400px 内）立即反向，远距离维持原方向
     */
    private void dodgeBullet() {
        if (enemyDistance < 400) {
            moveDirection *= -1;
        }
    }
}
