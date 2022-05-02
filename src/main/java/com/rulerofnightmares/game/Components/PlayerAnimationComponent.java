package com.rulerofnightmares.game.Components;

import com.almasb.fxgl.animation.Interpolators;
import com.almasb.fxgl.core.math.FXGLMath;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.texture.*;

import com.almasb.fxgl.time.TimerAction;
import com.rulerofnightmares.game.EntityType;
import javafx.geometry.Point2D;
import javafx.util.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.almasb.fxgl.dsl.FXGLForKtKt.getGameTimer;
import static com.almasb.fxgl.dsl.FXGLForKtKt.spawn;

public class PlayerAnimationComponent extends Component {

    private final static double DASH_TRANSLATE = 150;

    public static final Map<Integer, Integer> LEVELS_EXP_MAP = Map.of(1, 100, 2, 200, 3, 300, 4, 400, 5, 500, 6, 600);
    public static final double ATTACK_ANIMATION_DURATION = 0.5;
    private int speed = 0;
    private int v_speed = 0;
    private int isAttacking = 0;

    private boolean isAttacked;

    private int hp;

    private int xp;

    public int getMp() {
        return mp;
    }

    public void setMp(int mp) {
        this.mp = mp;
    }

    public void regenerateMp() {
        if (this.mp >= 100) return;
        if (this.mp + 5 >= 100) this.mp = 100;
        else this.mp += 5;
    }

    private int mp;

    private boolean animationLock;

    private int currentLevel;

    private AnimatedTexture texture;
    private AnimationChannel animIdle, animWalk, animAttack, animAttacked, animDeath;

    private int dashMultiplier;

    public PlayerAnimationComponent() {
        //animAttacked nie działa, nie wiem czemu
        animAttacked = new AnimationChannel(FXGL.image("player_sprite.png"),13,32,32,Duration.seconds(1),78,81);
        animIdle = new AnimationChannel(FXGL.image("player_sprite.png"), 13, 32, 32, Duration.seconds(1), 1, 1);
        animWalk = new AnimationChannel(FXGL.image("player_sprite.png"), 13, 32, 32, Duration.seconds(1), 0, 3);
        animAttack = new AnimationChannel(FXGL.image("player_sprite.png"),13,32,32,Duration.seconds(ATTACK_ANIMATION_DURATION),27,35);
        animDeath = new AnimationChannel(FXGL.image("player_sprite.png"),13,32,32,Duration.seconds(1),93,97);

        texture = new AnimatedTexture(animIdle);

        texture.setOnCycleFinished( () ->{
            isAttacking = 0;
        } );
    }
    public int getHp() {
        return this.hp;
    }

    public int getCurrentLevel() {
        return this.currentLevel;
    }

    public void setCurrentLevel(int lvl) {
        this.currentLevel = lvl;
    }

    public void incrementCurrentLevel() {
        this.currentLevel++;
    }

    public int getXp() {
        return this.xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public void incrementXp(int additionalXp) {
        this.xp += additionalXp;
    }

    public void regenerateHP() {
        if (this.hp < 100) {
            this.hp++;
        }
    }

    public boolean canAscend() {
        if (currentLevel == 7) return false;
        return this.xp >= LEVELS_EXP_MAP.get(currentLevel);
    }

    public void ascend() {
        int tempXp = LEVELS_EXP_MAP.get(currentLevel);//xp potrzebne do awansowania
        incrementCurrentLevel();
        setXp(this.xp - tempXp);
    }

    public boolean isAttacking() {
        return this.isAttacking == 1;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public void receiveDmg(int dmg) {
        this.isAttacked = true;
        this.hp -= dmg;
    }

  
    @Override
    public void onAdded() {
        entity.getTransformComponent().setScaleOrigin(new Point2D(16, 16));
        entity.getViewComponent().addChild(texture);
        this.isAttacked = false;
        this.hp = 100;
        this.xp = 0;
        this.mp = 0;
        this.currentLevel = 1;
        this.animationLock = false;
        this.dashMultiplier = 1;
        getGameTimer().runAtInterval(this::regenerateHP, Duration.seconds(2));
        getGameTimer().runAtInterval(this::regenerateMp, Duration.seconds(1));
    }

    @Override
    public void onUpdate(double tpf) {
        entity.translateX(speed * tpf * dashMultiplier);
        entity.translateY(v_speed * tpf * dashMultiplier);

        if (this.hp <= 0) {
            texture.playAnimationChannel(animDeath);
            getGameTimer().runOnceAfter(entity::removeFromWorld, Duration.seconds(1));
        }

        if (canAscend()) ascend();



        if (texture.getAnimationChannel() != animDeath) {
            if(isAttacking == 1 && texture.getAnimationChannel() != animAttack) {
                texture.playAnimationChannel(animAttack);
            }

//            if (isAttacked) {
//                //tutaj nie działa
//                texture.playAnimationChannel(animAttacked);
//                this.isAttacked = false;
//            }

            if (speed != 0 || v_speed != 0) {

                if (texture.getAnimationChannel() != animWalk&&isAttacking == 0) {
                    texture.loopAnimationChannel(animWalk);
                }

                speed = (int) (speed * 0.9);
                v_speed = (int) (v_speed * 0.9);

                if (FXGLMath.abs(speed) < 1 && FXGLMath.abs(v_speed) < 1) {
                    speed = 0;
                    v_speed = 0;
                    texture.loopAnimationChannel(animIdle);
                }
            }
            else if(isAttacking == 0 ) texture.loopAnimationChannel(animIdle);
        }

    }

    public void moveRight() {
        speed = 150;
        
        getEntity().setScaleX(1);
    }

    public void moveLeft() {
        speed = -150;

        getEntity().setScaleX(-1);
    }
    
    public void moveUp() {
        v_speed = -150;
    }

    public void moveDown() {
        v_speed = 150;
    }
    
    public void attack() {
        if (FXGL.getGameWorld().getEntitiesByType(EntityType.PLAYER_NORMAL_ATTACK).isEmpty()) {
            isAttacking = 1;
            Entity normalAttack = spawn("PlayerNormalAttack", entity.getCenter().getX() + 10 * entity.getScaleX(), entity.getCenter().getY());
            getGameTimer().runOnceAfter(normalAttack::removeFromWorld, Duration.seconds(ATTACK_ANIMATION_DURATION));
        }
    }

    private double getCorrectVerticalDashTranslation() {
        if (v_speed > 0) return DASH_TRANSLATE;
        else if (v_speed < 0) return -DASH_TRANSLATE;
        return 0;
    }

    public void dash() {
        //zakomentuj ifa by sprawdzić działanie
        if (mp < 20 || currentLevel < 2) return;
        this.mp -= 20;
//        FXGL.animationBuilder()
//                .setOnFinished(() -> {
//                    //coś to nie chce działać
//                })
//                .interpolator(Interpolators.EXPONENTIAL.EASE_OUT())
//                .duration(Duration.seconds(1))
//                .translate(entity)
//                .from(new Point2D(entity.getX() + (speed == 0 ? 0 : DASH_TRANSLATE * entity.getScaleX()),
//                        entity.getY() + getCorrectVerticalDashTranslation()))
//                .build().start();
        dashMultiplier = 7;
        getGameTimer().runOnceAfter(() -> {
            dashMultiplier = 1;
        }, Duration.seconds(0.25));
    }
}

