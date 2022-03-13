package com.varane.dao;


import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientUserCodeDeploymentConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.varane.models.Student;
import com.varane.repositories.StudentRepo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * This DAO is to insert/update student entries in databse
 * @author Sreeram Maram
 */
@Component
public class StudentDAO {
    private static final Log LOG = LogFactory.getLog(StudentDAO.class);

    @Autowired
    StudentRepo studentRepo;

    private HazelcastInstance hazelcastInstance = HazelcastClient.newHazelcastClient(getConfig());

    private ClientConfig getConfig(){
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getUserCodeDeploymentConfig().setEnabled(true);
        ClientUserCodeDeploymentConfig distCLConfig = clientConfig.getUserCodeDeploymentConfig();
        List<String> classNames  = new ArrayList<>();
        classNames.add("com.varane.models.Student");
        distCLConfig.setEnabled( true )
                        .setClassNames(classNames);
        return clientConfig;
    }

    public IMap<Integer, Student> getHazelcastStudentsMap() {
        return hazelcastInstance.getMap("students");
    }

    public Student findByIdCustom(@Param("id") Integer id) throws InterruptedException {
        Student student = getHazelcastStudentsMap().get(id);
        if(student != null){
            LOG.info(String.format("Fetched student from cache for id: %d", id));
            return student;
        }
        Thread.sleep(1000);
        student = studentRepo.findById2(id).orElse(null);
        if(student != null) {
            LOG.info(String.format("Inserting student into cache for id: %d", id));
            getHazelcastStudentsMap().put(id, student);
        } else{
            LOG.info(String.format("Student not found for id: %d", id));
        }
        return student;
    }

    public List<Student> getStudentsLessThanId(@Param("id") Integer id) throws InterruptedException {
        Thread.sleep(1000);
        return studentRepo.getStudentsLessThanId(id);
    }

    public Student findById(Integer id) throws InterruptedException {
        Thread.sleep(1000);
        Student student = studentRepo.findById(id).orElse(null);
        return student;
    }

    public Student insertingStudent(Integer id, String name, String contact) throws InterruptedException {
        LOG.info("New Student ADD request with id: " + id + " Name: " + name);
        Student student = this.findById(id);
        if(student != null) {
            LOG.info(String.format("Student already exists with id: %d", student.getId()));
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format("Student with id:%d - Already Exists ", student.getId()));
        }
        Thread.sleep(1000);
        studentRepo.insertingStudent(id, name, contact);
        student = this.findById(id);
        LOG.info(String.format("Adding student to cache with id: %d", student.getId()));
        getHazelcastStudentsMap().put(student.getId(), student);
        return student;
    }

    public List<Student> findAll() throws InterruptedException {
        Thread.sleep(1000);
        return (List<Student>) studentRepo.findAll();
    }

    public HazelcastInstance getHazelcastInstance() {
        return hazelcastInstance;
    }
}
