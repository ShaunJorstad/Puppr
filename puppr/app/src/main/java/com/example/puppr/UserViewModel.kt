package com.example.puppr

import android.content.Context
import android.text.Editable
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

data class User(
    var name: String? = "",
    var email: String? = "",
    var address: String? = "",
    var phone: String? = "",
    var bio: String? = "",
    var agePrefHigh: Int? = -1,
    var agePrefLow: Int? = -1,
    var preferredBreeds: List<String>? = null,
    var likedDogs: List<String>? = null,
    var dislikedDogs: List<String>? = null
)

data class Shelter(
    var name: String? = "",
    var email: String? = "",
    var address: String? = "",
    var phone: String? = "",
    var bio: String? = "",
    var dogs: List<String>? = listOf(),
    var photos: List<String>? = null,
    var website: String? = ""
)

data class Dog(
    var name: String? = "",
    var bio: String? = "",
    var breed: String? = "",
    var color: String? = "",
    var age: String? = "",
    var shelter: String? = "",
    var health: String? = "",
    var photo: Array<String>? = arrayOf()
)

class UserViewModel : ViewModel() {
    private val TAG = "UserViewModel"
    var userID: String? = ""
    var userType = ""
    var tempEmail = ""
    var tempPassword = ""
    var shelter: Shelter
    var user: User
    var auth: FirebaseAuth
    var database: FirebaseFirestore
    var storage: FirebaseStorage
    var dog: Dog = Dog()
    var nextDog: Dog = Dog()
    var dogID: String = "test"
    var nextDogID: String = "test2"
    var savedDogsID: String? = null
    var dogIDs: ArrayList<String> = arrayListOf()

    init {
        Log.i(TAG, "View Model Created")
        auth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        user = User()
        shelter = Shelter()

        Thread(Runnable {
            Thread.sleep(2000)
            fillDogIDs(true)
        }).start()
    }

    fun loadDog(firstTime: Boolean = false): Boolean {

        if (!firstTime) {

            dogID = nextDogID
            nextDogID = if (dogIDs.isNotEmpty()) dogIDs[0] else "Dogs Done"
            if (dogIDs.isNotEmpty()) {
                dogIDs.removeAt(0)
            }

            dog.name = nextDog.name
            dog.bio = nextDog.bio
            dog.breed = nextDog.breed
            dog.color = nextDog.color
            dog.age = nextDog.age
            dog.health = nextDog.health
            dog.photo = nextDog.photo
            dog.shelter = nextDog.shelter

        } else {

            val docRef = database.collection("dogs").document(dogID)
            docRef.get()
                .addOnSuccessListener { document ->

                    dog.name = document.data?.get("name").toString()
                    dog.bio = document.data?.get("bio").toString()
                    dog.breed = document.data?.get("breed").toString()
                    dog.color = document.data?.get("color").toString()
                    dog.age = document.data?.get("age").toString()
                    dog.health = document.data?.get("health").toString()
                    dog.photo = document.data?.get("photos").toString()
                        .replace("[", "").replace("]", "").replace(" ", "").split(",").toTypedArray()

                    val docRef2 = database.collection("shelter")
                        .document(document.data?.get("shelter").toString())
                    docRef2.get()
                        .addOnSuccessListener { document2 ->
                            dog.shelter = document2.data?.get("name").toString()
                        }
                }
            dog.shelter = ""
        }

        val docRef = database.collection("dogs").document(nextDogID)
        docRef.get()
            .addOnSuccessListener { document ->

                nextDog.name = document.data?.get("name").toString()
                nextDog.bio = document.data?.get("bio").toString()
                nextDog.breed = document.data?.get("breed").toString()
                nextDog.color = document.data?.get("color").toString()
                nextDog.age = document.data?.get("age").toString()
                nextDog.health = document.data?.get("health").toString()
                nextDog.photo = document.data?.get("photos").toString()
                    .replace("[", "").replace("]", "").replace(" ", "").split(",").toTypedArray()

                Log.d("Dog Test", "Shelter ID: ${document.data?.get("shelter")}")
                val docRef2 = database.collection("shelter")
                    .document(document.data?.get("shelter").toString())
                docRef2.get()
                    .addOnSuccessListener { document2 ->
                        Log.d("Dog Test", "Shelter Name: ${document2.data?.get("name")}")
                        nextDog.shelter = document2.data?.get("name").toString()
                    }
                nextDog.shelter = ""
            }
        return true
    }

    fun fillDogIDs(fillDogID: Boolean = false) {

        val docRef = database.collection("dogs")
        Log.d("Dog Add", "Fill Dog IDs Entered")

        docRef.get()
            .addOnSuccessListener { documents ->

                for (document in documents) {

                    if (user.likedDogs != null && user.dislikedDogs != null) {

                        Log.d("Dog Add", "Comparison: ${!user.likedDogs!!.contains(document.id) and !user.dislikedDogs!!.contains(document.id)}")
                        if (!user.likedDogs!!.contains(document.id) and !user.dislikedDogs!!.contains(document.id)) {

                            if (!dogIDs.contains(document.id)) {
                                Log.d("Dog Add", "Dog Added - ID: ${document.id}")
                                dogIDs.add(document.id)
                            }
                        }
                    }
                }

                dogID = if (fillDogID && dogIDs.isNotEmpty()) dogIDs[0] else dogID
                nextDogID = if (fillDogID && dogIDs.size >= 2) dogIDs[1] else nextDogID
                loadDog(true)
            }
            .addOnFailureListener { exception ->
                Log.d("YERT", "get failed with $exception")
            }
    }

}