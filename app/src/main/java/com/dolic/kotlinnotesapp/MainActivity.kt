package com.dolic.kotlinnotesapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.dolic.kotlinnotesapp.adapters.NotesRecyclerAdapter
import com.dolic.kotlinnotesapp.databinding.ActivityMainBinding
import com.dolic.kotlinnotesapp.entities.Note
import com.dolic.kotlinnotesapp.selectionUtils.NoteItemKeyProvider
import com.dolic.kotlinnotesapp.selectionUtils.NoteItemsDetailsLookup
import com.dolic.kotlinnotesapp.viewmodels.NotesViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.widget.addTextChangedListener
import com.dolic.kotlinnotesapp.fragments.NotesFragment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NotesFragment.FragmentActionModeStart {

    private lateinit var binding: ActivityMainBinding
    //private lateinit var viewmodel: NotesViewModel

    private var tracker: SelectionTracker<Note>? = null

    private var actionMode: ActionMode? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //viewmodel = ViewModelProvider(this).get(NotesViewModel::class.java)

        /*setSupportActionBar(binding.toolbar)
        actionBar?.setDisplayShowTitleEnabled(false)*/

        /*viewmodel.searchJob?.let {
            if(it.isCancelled)
                it.start()
        }*/



        /*val adapter = NotesRecyclerAdapter()
        binding.notesRecycler.apply {
            this.adapter = adapter
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        }

        setupTracker(adapter)

        viewmodel.getAllNotes().observe(this) {
            adapter.submitList(it)
        }

        *//*binding.fab.setOnClickListener {
            viewmodel.insertNote(Note(noteTitle = "Test title", noteDesc = binding.searchview.query.toString()))
        }*//*

        binding.searchNotesEdittext.addTextChangedListener { text: Editable? ->
            text?.let {
                viewmodel.searchJob = GlobalScope.launch {
                    val notes = viewmodel.searchNotes(it.toString().trim())
                    adapter.submitList(notes)
                }
            }
        }*/


    }

    /**
     * Function used to create SelectionTracker
     * @param adapter Recyclerview adapter which is passed to NoteItemsDetailsLookup
     *
     * @see NoteItemsDetailsLookup
     */
    /*fun setupTracker(adapter: NotesRecyclerAdapter) {
        tracker = SelectionTracker.Builder<Note>(
            "noteSelectionTracker",
            binding.notesRecycler,
            NoteItemKeyProvider(adapter),
            NoteItemsDetailsLookup(binding.notesRecycler),
            StorageStrategy.createParcelableStorage(Note::class.java)
        )
            .withSelectionPredicate(
                SelectionPredicates.createSelectAnything()
            ).build()
        adapter.setTracker(tracker!!)

        tracker?.addObserver(object : SelectionTracker.SelectionObserver<Note>() {

            override fun onItemStateChanged(key: Note, selected: Boolean) {
                super.onItemStateChanged(key, selected)
            }

            override fun onSelectionRefresh() {
                super.onSelectionRefresh()
            }

            override fun onSelectionChanged() {
                super.onSelectionChanged()

                val items = tracker?.selection!!.size()
                if(items > 0) {
                    if (actionMode == null) {
                        if (tracker != null) {
                            if (items > 0)
                                actionMode = setupActionMode(tracker)
                        }

                    } else actionMode?.title = "Selected ${tracker?.selection?.size()!!}"
                } else actionMode?.finish()

            }

            override fun onSelectionRestored() {
                super.onSelectionRestored()
            }
        })

    }*/

    override fun startActionMode(callback: ActionMode.Callback): ActionMode? {
        return startSupportActionMode(callback)
    }

    override fun onPause() {
        super.onPause()
        //viewmodel.searchJob?.cancel()
    }

    fun setupActionMode(tracker: SelectionTracker<Note>?): ActionMode? {

        val actionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                mode?.menuInflater?.inflate(R.menu.delete_toolbar_menu, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return true
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                tracker?.clearSelection()
                actionMode = null
            }
        }
        return startSupportActionMode(actionModeCallback)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        //tracker?.onRestoreInstanceState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //tracker?.onSaveInstanceState(outState)
    }
}