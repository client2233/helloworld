package com.nku.helloworld.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.nku.helloworld.R
import com.nku.helloworld.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        val tasks = listOf(
            PlanTaskItem(getString(R.string.plan_task_1_title), getString(R.string.plan_task_1_meta), getString(R.string.plan_status_todo), R.color.status_todo),
            PlanTaskItem(getString(R.string.plan_task_2_title), getString(R.string.plan_task_2_meta), getString(R.string.plan_status_pending), R.color.status_pending),
            PlanTaskItem(getString(R.string.plan_task_3_title), getString(R.string.plan_task_3_meta), getString(R.string.plan_status_todo), R.color.status_todo),
            PlanTaskItem(getString(R.string.plan_task_4_title), getString(R.string.plan_task_4_meta), getString(R.string.plan_status_done), R.color.status_done),
        )
        binding.planRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.planRecyclerView.adapter = PlanTaskAdapter(tasks)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}