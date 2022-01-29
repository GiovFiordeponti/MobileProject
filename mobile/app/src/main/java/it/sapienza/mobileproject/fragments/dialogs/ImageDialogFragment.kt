package it.sapienza.mobileproject.fragments.dialogs

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.google.firebase.storage.FirebaseStorage
import it.sapienza.mobileproject.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class ImageDialogFragment : WebDialogFragment() {

    companion object {

        const val TAG = "PlayDialogFragment"

        private const val KEY_TITLE = "KEY_TITLE"
        private const val KEY_SUBTITLE = "KEY_SUBTITLE"
        private const val KEY_USER_ID = "KEY_USER_ID"


        fun newInstance(title: String, subTitle: String, userId: String): ImageDialogFragment {
            val args = Bundle()
            args.putString(KEY_TITLE, title)
            args.putString(KEY_SUBTITLE, subTitle)
            args.putString(KEY_USER_ID, userId)
            val fragment =
                ImageDialogFragment()
            fragment.arguments = args
            return fragment
        }

    }


    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

    private lateinit var photoURI: Uri

    private val storage = FirebaseStorage.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    if (result.data != null && result.data!!.data != null) {
                        Log.d(TAG, "Image is loaded from library")
                        uploadPictureToStorage(result.data!!.data!!)
                    } else if (this::photoURI.isInitialized) {
                        Log.d(TAG, "Image is loaded from camera (i.e. private storage)")
                        uploadPictureToStorage(photoURI)
                    }
                }
            }
    }

    /** The system calls this to get the DialogFragment's layout, regardless
    of whether it's being displayed as a dialog or an embedded fragment. */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout to use as dialog or embedded fragment
        return inflater.inflate(R.layout.dialog_image_capture, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView(view)
    }


    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

    }

    override fun setupView(view: View) {
        //view.background.alpha = 15
        setTopBottomView(
            view,
            KEY_TITLE,
            KEY_SUBTITLE
        )
        view.findViewById<Button>(R.id.btnPositive).visibility = View.GONE
    }

    override fun setupClickListeners(view: View) {

        view.findViewById<Button>(R.id.btnNegative).setOnClickListener {
            dismiss()//deleteRequest()//deleteRequest()
        }

        view.findViewById<Button>(R.id.button_camera).setOnClickListener {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                photoURI = getPhotoFileUri()
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
                    cameraLauncher.launch(takePictureIntent)
                }
            }
        }

        view.findViewById<Button>(R.id.button_library).setOnClickListener {
            val takePicture = Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )

            cameraLauncher.launch(takePicture)
        }
    }

    private fun getPhotoFileUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_${timeStamp}.png"

        var uri: Uri? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = requireContext().contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "pongchallenge/$fileName")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/")
            }

            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        }

        return uri ?: getUriForPreQ(fileName)
    }

    private fun getUriForPreQ(fileName: String): Uri {
        Log.d(TAG, "getting photo pre q")
        val dir = requireContext().getExternalFilesDir("")
        val photoFile = File(dir, "$fileName")
        if (photoFile.parentFile?.exists() == false) photoFile.parentFile?.mkdir()
        return FileProvider.getUriForFile(
            requireContext(),
            "it.sapienza.mobileproject.fileprovider",
            photoFile
        )
    }

    private fun uploadPictureToStorage(filePath: Uri) {
        Log.d(TAG, "firebaseStorage: picture found")
        pongViewModel.setPictureUpdated(filePath)
        val fileReference =
            storage.reference.child("${arguments?.getString(KEY_USER_ID)}/profile_picture.png")
        fileReference.putFile(filePath)
            .addOnSuccessListener {
                Log.d(TAG, "firebaseStorage: loading ok")
                dismiss()

            }
            .addOnFailureListener {
                Log.d(TAG, "firebaseStorage: loading error")
                pongViewModel.setPictureUpdated(null)
            }
    }
}