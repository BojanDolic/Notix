package com.dolic.kotlinnotesapp.fragments

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.dolic.kotlinnotesapp.R
import com.dolic.kotlinnotesapp.adapters.NotesRecyclerAdapter
import com.dolic.kotlinnotesapp.databinding.FragmentNotesBinding
import com.dolic.kotlinnotesapp.entities.Note
import com.dolic.kotlinnotesapp.onSearchTextChanged
import com.dolic.kotlinnotesapp.selectionUtils.NoteItemKeyProvider
import com.dolic.kotlinnotesapp.selectionUtils.NoteItemsDetailsLookup
import com.dolic.kotlinnotesapp.setSelectionChangedObserver
import com.dolic.kotlinnotesapp.util.Constants
import com.dolic.kotlinnotesapp.viewmodels.NotesViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Fragment which shows all notes added to the app
 */

@AndroidEntryPoint
class NotesFragment : Fragment() {

    interface FragmentActionModeStart {
        fun startActionMode(callback: ActionMode.Callback): ActionMode?
    }

    lateinit var actionModeInterface: FragmentActionModeStart

    var _binding: FragmentNotesBinding? = null
    val viewmodel by viewModels<NotesViewModel>()

    private var tracker: SelectionTracker<Note>? = null

    private val notesAdapter = NotesRecyclerAdapter()

    private var searchView: SearchView? = null

    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        NavigationUI.setupWithNavController(
            binding.notesToolbar,
            findNavController()
        )

        (activity as AppCompatActivity).setSupportActionBar(binding.notesToolbar)

        setupRecycler()
        setupSelectionTracker()

        viewmodel.actionMode?.let {
            viewmodel.actionMode = setupActionMode(tracker)
            viewmodel.actionMode?.invalidate()
        }

        viewmodel.searchJob?.let { job ->
            viewmodel.searchJob = lifecycleScope.launch(Dispatchers.IO) {
                 val notes = viewmodel.searchNotes(viewmodel.searchQuery)
                notesAdapter.submitList(notes)
            }
        }

        viewmodel.deleteJob?.let {
           if(it.isCancelled) {
               for(note in viewmodel.notesToDelete) {
                   viewmodel.deleteJob = lifecycleScope.launch {
                       viewmodel.deleteNote(note).await()
                       viewmodel.notesToDelete.remove(note)
                   }
               }
           }
        }


        viewmodel.getAllNotes().observe(viewLifecycleOwner, { notes ->
            notesAdapter.submitList(notes)
        })

        // New note button click
        binding.fab.setOnClickListener {
            findNavController().navigate(
                NotesFragmentDirections.actionNotesFragmentToNewNoteFragment(
                    Constants.ADD_NOTE_NAV,
                    resources.getString(R.string.create_note_fragment_title)))
        }


        setHasOptionsMenu(true)

    }

    /**
     * Function used to create SelectionTracker for recyclerview
     * @see SelectionTracker
     */
    fun setupSelectionTracker() {

        tracker = SelectionTracker.Builder<Note>(
            "noteSelectionTracker",
            binding.notesRecycler,
            NoteItemKeyProvider(notesAdapter),
            NoteItemsDetailsLookup(binding.notesRecycler),
            StorageStrategy.createParcelableStorage(Note::class.java)
        )
            .withSelectionPredicate(
                SelectionPredicates.createSelectAnything()
            ).build()
        notesAdapter.setTracker(tracker!!)

        /**
         * This is an extension function for SelectionTracker
         * @see setSelectionChangedObserver
         */
        tracker?.setSelectionChangedObserver { selectionSize ->

            if(selectionSize > 0) {
                if (viewmodel.actionMode == null) {
                    if (tracker != null) {
                        if (selectionSize > 0)
                            viewmodel.actionMode = setupActionMode(tracker)
                    }

                } else {
                    viewmodel.actionMode?.title =
                        resources.getQuantityString(
                            R.plurals.selection_tracker_selected_title_plurals,
                            selectionSize,
                            selectionSize)
                }
            } else viewmodel.actionMode?.finish()

        }

        /*tracker?.addObserver(object : SelectionTracker.SelectionObserver<Note>() {

            override fun onItemStateChanged(key: Note, selected: Boolean) {
                super.onItemStateChanged(key, selected)
            }

            override fun onSelectionRefresh() {
                super.onSelectionRefresh()
            }

            override fun onSelectionChanged() {
                super.onSelectionChanged()

            }

            override fun onSelectionRestored() {
                super.onSelectionRestored()
            }
        })*/

    }

    /**
     * Function which sets up action mode used for deleting notes
     *
     * @param tracker Selection tracker for recyclerview
     * @return ActionMode object to use
     */
    fun setupActionMode(tracker: SelectionTracker<Note>?): ActionMode? {
        val actionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                mode?.menuInflater?.inflate(R.menu.delete_toolbar_menu, menu)

                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                val selectedSize = tracker?.selection?.size()

                if (selectedSize != null) {
                    if(selectedSize > 0) {
                        mode?.title =
                            resources.getQuantityString(
                                R.plurals.selection_tracker_selected_title_plurals,
                                selectedSize,
                                selectedSize
                            )
                    }
                }
                return true
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {

                when(item?.itemId) {

                    R.id.delete_notes -> {
                        viewmodel.notesToDelete = tracker?.selection?.toList()?.toMutableList()!!
                        for(note in viewmodel.notesToDelete) {
                            viewmodel.deleteJob = lifecycleScope.launch {
                                viewmodel.deleteNote(note).await()
                                viewmodel.notesToDelete.remove(note)
                            }
                        }
                        tracker.clearSelection()
                        mode?.finish()
                        viewmodel.actionMode = null

                        Snackbar.make(binding.root,
                        "Items deleted",
                        Snackbar.LENGTH_SHORT).apply {
                            anchorView = binding.fab
                        }.show()
                    }

                }

                return false
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                tracker?.clearSelection()
                viewmodel.actionMode = null
            }
        }
        return actionModeInterface.startActionMode(actionModeCallback)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        tracker?.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        tracker?.onRestoreInstanceState(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.toolbar_search_menu, menu)

        val searchItem = menu.findItem(R.id.toolbar_search)
        searchView = searchItem.actionView as SearchView

        val searchQuery = viewmodel.searchQuery

        if(searchQuery.isNotEmpty()) {
            searchItem.expandActionView()
            searchView?.setQuery(searchQuery, false)
        }

        searchView?.onSearchTextChanged { searchString ->
            viewmodel.searchJob = lifecycleScope.launch(Dispatchers.IO) {
                viewmodel.searchQuery = searchString
                val notes = viewmodel.searchNotes(viewmodel.searchQuery)
                notesAdapter.submitList(notes)
            }
        }

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        actionModeInterface = context as FragmentActionModeStart
    }

    /**
     * Sets up recyclerview when fragment is created
     * Based on device rotation it determines spancount for StaggeredGridLayout
     */
    fun setupRecycler() {

        var spanCount = 2

        if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            spanCount = 3

        binding.notesRecycler.apply {
            this.adapter = notesAdapter
            layoutManager = StaggeredGridLayoutManager(
                spanCount,
                StaggeredGridLayoutManager.VERTICAL)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        searchView?.setOnQueryTextListener(null)

        viewmodel.deleteJob?.cancel()
        viewmodel.searchJob?.cancel()

    }

}