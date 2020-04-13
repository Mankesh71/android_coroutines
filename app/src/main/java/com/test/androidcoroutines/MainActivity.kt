package com.test.androidcoroutines

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import java.util.concurrent.CancellationException

class MainActivity : AppCompatActivity() {

    private val RESULT_1 = "Result #1"
    private val RESULT_2 = "Result #2"
    private val PROGRESS_MAX = 100
    private val PROGRESS_START = 0
    private val JOB_TIME = 4000
    private lateinit var job: CompletableJob

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button_click_me.setOnClickListener {
            // IO - for network call, main - to call on main thread and default for any heavy work like filter a list
            CoroutineScope(IO).launch {
                fakeApiRequest()
            }
        }

        button_start_job.setOnClickListener {
            if (!::job.isInitialized) {
                initJob()
            }
            progress_bar_job.startOrCancelJob(job = job)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun setDataToTextView(input: String) {
        val newText = text_view_show_data.text.toString() + "\n$input"
        text_view_show_data.text = newText
    }

    private suspend fun setDataOnMainThread(input: String) {
        /* Two ways to call coroutine on main thread
            CoroutineScope(Main).launch { setDataToTextView(input = input) }
         */
        withContext(Main) {
            setDataToTextView(input = input)
        }
    }

    private suspend fun fakeApiRequest() {
        val result = getResult1FromApi()
        setDataOnMainThread(input = result)
        println("Debug: $result")

        val result2 = getResult2FromApi()
        setDataOnMainThread(input = result2)
    }

    private suspend fun getResult1FromApi(): String {
        logMethod("getResult1FromApi")
        delay(1000)
        Thread.sleep(1000)
        return  RESULT_1
    }

    private suspend fun getResult2FromApi(): String {
        logMethod(methodName = "getResult2FromApi")
        delay(2000)
        return RESULT_2
    }

    private fun logMethod(methodName: String) {
        println("Debug: $methodName - ${Thread.currentThread()
            .name}")
    }

    private fun initJob() {
        button_start_job.text = getString(R.string.start_job)
        updateStatusTextView(input = "")
        job = Job()
        job.invokeOnCompletion { it ->
            it?.message.let {
                var msg = it
                if (msg.isNullOrBlank()) {
                    msg = "Unknown cancellation error"
                }
                println("$job was cancelled. Reason is $msg")
                updateStatusTextView(input = msg)
            }
        }
        progress_bar_job.max = PROGRESS_MAX
        progress_bar_job.progress = PROGRESS_START
    }

    private fun updateStatusTextView(input: String) {
        GlobalScope.launch(Main) {
            text_view_job_status.text = input
        }
    }

    private fun ProgressBar.startOrCancelJob(job: Job) {
        if (this.progress > 0) {
            resetJob()
        } else {
            button_start_job.text = getString(R.string.cancel_job)
            updateStatusTextView(input = "")
            CoroutineScope(IO + job).launch {
                println("$this coroutine is active with job $job")
                for (i in PROGRESS_START..PROGRESS_MAX) {
                    delay((JOB_TIME / PROGRESS_MAX).toLong())
                    this@startOrCancelJob.progress = i
                    updateStatusTextView(input = "Job start: $i/$PROGRESS_MAX ")
                }
                updateStatusTextView(input = "Job completed")
            }
        }
    }

    private fun resetJob() {
        if (job.isActive || job.isCompleted) {
            job.cancel(CancellationException("Reset job"))
        }
        initJob()
    }
}
