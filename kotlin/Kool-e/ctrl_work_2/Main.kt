package ctrl_work_2

import de.fabmax.kool.KoolApplication           // KoolApplication - запускает Kool-приложение (окно + цикл рендера)
import de.fabmax.kool.addScene                  // addScene - функция "добавь сцену" в приложение (у тебя она просила отдельный импорт)
import de.fabmax.kool.math.Vec3f                // Vec3f - 3D-вектор (x, y, z), как координаты / направление
import de.fabmax.kool.math.deg                  // deg - превращает число в "градусы" (угол)
import de.fabmax.kool.modules.audio.synth.SampleNode
import de.fabmax.kool.scene.*                   // scene.* - Scene, defaultOrbitCamera, addColorMesh, lighting и т.д.
import de.fabmax.kool.modules.ksl.KslPbrShader  // KslPbrShader - готовый PBR-шейдер (материал)
import de.fabmax.kool.util.Color                // Color - цвет (RGBA)
import de.fabmax.kool.util.Time                 // Time.deltaT - сколько секунд прошло между кадрами
import de.fabmax.kool.pipeline.ClearColorLoad   // ClearColorLoad - режим: "не очищай экран, оставь то что уже нарисовано"
import de.fabmax.kool.modules.ui2.*             // UI2: addPanelSurface, Column, Row, Button, Text, dp, remember, mutableStateOf
import de.fabmax.kool.physics.joints.DistanceJoint
import jdk.jfr.DataAmount
import jdk.jfr.StackTrace

fun main() = KoolApplication {
    val y = 0f

    addScene {
        defaultOrbitCamera()

        addColorMesh {
            generate { cube { colored() } }

            shader = KslPbrShader {
                color {
                    vertexColor()
                }
                metallic(0.7f)
                roughness(0.6f)
            }
        }


        lighting.singleDirectionalLight {
            setup(Vec3f(-1f, -1f, -1f))
            setColor(Color.WHITE, 5f)
        }
    }

    addScene {
        setupUiScene(ClearColorLoad)

        addPanelSurface {
            modifier
                .align(AlignmentX.Start, AlignmentY.Top)
                .margin(20.dp)
                .background(RoundRectBackground(Color(0f, 0f, 0f, 0.5f), 20.dp))
                .padding(12.dp)

            Column{
                Button ("Повернуть куб вправо") {
                    modifier
                        .margin(end = 8.dp)
                        .onClick {
                            transform.rotate(10f.deg * Time.deltaT, Vec3f.X_AXIS)
                        }
                }
                Button ("Повернуть куб влево") {
                    modifier
                        .margin(end = 8.dp)
                        .onClick {
                            transform.rotate((-10f).deg * Time.deltaT, Vec3f.X_AXIS)
                        }
                }
                Button ("добавить куб") {
                    modifier
                        .margin(end = 8.dp)
                        .onClick {
                            addColorMesh {
                                generate { cube { colored() } }

                                shader = KslPbrShader {
                                    color {
                                        vertexColor()
                                    }
                                    metallic(0.7f)
                                    roughness(0.6f)
                                }

                                transform.translate(0f, y + 2f, 0f)

                            }
                            val y =+ 2f
                        }
                }
            }
        }
    }
}

// 1.1) b
// 1.2) c
// 1.3) a
// 1.4) a
