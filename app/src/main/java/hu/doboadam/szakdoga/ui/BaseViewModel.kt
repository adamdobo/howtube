package hu.doboadam.szakdoga.ui

import android.arch.lifecycle.ViewModel
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.appindexing.Indexable
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import hu.doboadam.szakdoga.extensions.getMostFittingThumbnailUrl
import hu.doboadam.szakdoga.model.FirestoreRepository
import hu.doboadam.szakdoga.model.YoutubeVideo
import timber.log.Timber

abstract class BaseViewModel : ViewModel() {

    protected var listener: ListenerRegistration = ListenerRegistration { }
    private var appIndexListener: ListenerRegistration = ListenerRegistration { }

    companion object {
        private const val VIDEOS = "videos"
    }

    open fun startListeningToDbChanges() {
        updateIndexOnVideoUpload()
    }

    open fun stopListeningToDbChanges() {
        appIndexListener.remove()
        listener.remove()
    }

    private fun updateIndexOnVideoUpload() {
        appIndexListener = FirestoreRepository.listenToCollectionChanges(VIDEOS) { snapshot: QuerySnapshot?, exception: FirebaseFirestoreException? ->
            if (exception != null) {
                Timber.e("Listening failed with $exception")
            }
            if (snapshot != null) {
                for (docChange in snapshot.documentChanges) {
                    when (docChange.type) {
                        DocumentChange.Type.ADDED -> {
                            val video = docChange.document.toObject(YoutubeVideo::class.java)
                            updateFirebaseAppIndex(video)
                        }
                        DocumentChange.Type.MODIFIED -> { /* do nothing */
                        }
                        DocumentChange.Type.REMOVED -> { /* do nothing */
                        }
                    }
                }
            }
        }
    }

    private fun updateFirebaseAppIndex(video: YoutubeVideo) {
        val firebaseAppIndex = FirebaseAppIndex.getInstance()
        firebaseAppIndex.update(Indexable.Builder()
                .setUrl("http://doboadam.hu/recipe/${video.id}")
                .setName(video.snippet.title)
                .setImage(video.snippet.thumbnails.getMostFittingThumbnailUrl())
                .build())
    }

}