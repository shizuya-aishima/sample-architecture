package com.example.item;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ItemApplicationTests {

	@BeforeAll
	public static void befor() {
		Firestore FS = FirestoreOptions.getDefaultInstance().toBuilder()
						.setProjectId("sample")
						.setHost("localhost:8888")
						.setCredentials(new FirestoreOptions.EmulatorCredentials())
						.setCredentialsProvider(FixedCredentialsProvider.create(new FirestoreOptions.EmulatorCredentials()))
						.build()
						.getService();
		System.out.println(FS.document("doc/test"));
	}

	@Test
	void contextLoads() {
	}

}
