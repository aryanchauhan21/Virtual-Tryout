package com.leotarius.VirtualWoodPalace

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.collision.Box
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.CompletableFuture

private const val BOTTOM_SHEET_PEEK_HEIGHT = 50f
private const val DOUBLE_TAP_TOLERANCE_MS = 1000L

class MainActivity : AppCompatActivity() {

    private lateinit var selectedModel: Model
    private lateinit var arFragment: ArFragment

    private val models = mutableListOf<Model>(
        Model(R.drawable.chair, "Chair", R.raw.chair),
        Model(R.drawable.oven, "Oven", R.raw.oven),
        Model(R.drawable.piano, "Piano", R.raw.piano),
        Model(R.drawable.table, "Table", R.raw.table)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arFragment = fragment as ArFragment
        setUpBottomSheet()
        setUpRecyclerView()
        setUpDoubleTapPlaneListener()
        getCurrentScene().addOnUpdateListener {
            rotateViewNodesTowardsUser()
        }
    }

    private fun setUpDoubleTapPlaneListener(){
        var firstTapTime = 0L
        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            if(firstTapTime == 0L){
                firstTapTime = System.currentTimeMillis()
            } else if(firstTapTime - System.currentTimeMillis() < DOUBLE_TAP_TOLERANCE_MS){
                firstTapTime = 0L
                loadModel { modelRenderable, viewRenderable ->
                    addNodeToScene(hitResult.createAnchor(), modelRenderable, viewRenderable)
                }
            } else{
                firstTapTime = System.currentTimeMillis()
            }
        }
    }

    private fun setUpRecyclerView() {
        rvModels.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvModels.adapter = ModelAdapter(this, models).apply {
            selectedModel.observe(this@MainActivity, Observer {
                this@MainActivity.selectedModel = it
                tvModel.text = "Models (${it.title})"
            })

        }
    }

    private fun setUpBottomSheet() {
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            BOTTOM_SHEET_PEEK_HEIGHT,
            resources.displayMetrics
        ).toInt()

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                bottomSheet.bringToFront()
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {}

        })
    }

    private fun createDeleteButton(): Button {
        return Button(this).apply {
            text = "Delete"
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)
        }
    }

    private fun rotateViewNodesTowardsUser(){
        for(node in viewNodes){
            if(node.renderable != null){
                val cameraPosition = getCurrentScene().camera.worldPosition
                val viewModelpos = node.worldPosition
                val dir = Vector3.subtract(cameraPosition, viewModelpos)
                node.worldRotation = Quaternion.lookRotation(dir, Vector3.up())
            }
        }
    }

    private fun getCurrentScene() = arFragment.arSceneView.scene

    private var viewNodes = mutableListOf<Node>()

    private fun addNodeToScene(
        anchor: Anchor,
        modelRenderable: ModelRenderable,
        viewRenderable: ViewRenderable
    ) {
        val anchorNode = AnchorNode(anchor)
        val modelNode = TransformableNode(arFragment.transformationSystem).apply {
            scaleController.minScale = 0.1f
            scaleController.maxScale = 0.3f

            renderable = modelRenderable
            setParent(anchorNode)
            getCurrentScene().addChild(anchorNode)
            select()
        }
        val viewNode = Node().apply {
            // its initially null because delete button will not be visible initially and will
            // only be visible when model is tapped (we set the renderable inside the modelNode tap listener)
            renderable = null
            setParent(modelNode)
            val box = modelNode.renderable?.collisionShape as Box
            localPosition = Vector3(0f, box.size.y, 0f)
            (viewRenderable.view as Button).setOnClickListener {
                getCurrentScene().removeChild(anchorNode)
                viewNodes.remove(this)
            }
        }
        modelNode.setOnTapListener { _, _ ->
            if(!modelNode.isTransforming) {
                if(viewNode.renderable == null){
                    viewNode.renderable = viewRenderable
                } else{
                    viewNode.renderable = null
                }
            }
        }
        viewNodes.add(viewNode)
    }

    private fun loadModel(callback: (ModelRenderable, ViewRenderable) -> Unit) {
        val modelRenderable = ModelRenderable.builder()
            .setSource(this, selectedModel.modelResourceId)
            .build()
        val viewRenderable = ViewRenderable.builder()
            .setView(this, createDeleteButton())
            .build()

        CompletableFuture.allOf(modelRenderable, viewRenderable)
            .thenAccept {
                callback(modelRenderable.get(), viewRenderable.get())
            }
            .exceptionally {
                Toast.makeText(this, "Error loading model", Toast.LENGTH_SHORT).show()
                null
            }
    }
}