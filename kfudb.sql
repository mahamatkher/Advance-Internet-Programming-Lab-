-- =============================================================
--  King Faisal University – Chad
--  Academic Information System v2.0
--  Database: kfudb
--  Generated: 2025
--
--  Tables (16):
--    users, departments, students, faculty,
--    courses, timetable, attendance, results,
--    assignments, submissions, fees,
--    hostel_rooms, hostel_allotments,
--    library_books, library_issues, notices
-- =============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -------------------------------------------------------------
-- Drop & create database
-- -------------------------------------------------------------
DROP DATABASE IF EXISTS kfudb;
CREATE DATABASE kfudb
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE kfudb;

-- =============================================================
-- 1. USERS  (central auth table – all roles share it)
-- =============================================================
CREATE TABLE users (
    user_id    INT          NOT NULL AUTO_INCREMENT,
    uid        VARCHAR(50)  NOT NULL UNIQUE,          -- e.g. KFU-2024-001
    password   VARCHAR(255) NOT NULL,
    role       ENUM('admin','faculty','student') NOT NULL DEFAULT 'student',
    full_name  VARCHAR(150) NOT NULL,
    email      VARCHAR(150)          DEFAULT NULL,
    phone      VARCHAR(20)           DEFAULT NULL,
    is_active  TINYINT(1)   NOT NULL DEFAULT 1,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    INDEX idx_users_uid  (uid),
    INDEX idx_users_role (role)
) ENGINE=InnoDB;

-- =============================================================
-- 2. DEPARTMENTS
-- =============================================================
CREATE TABLE departments (
    dept_id   INT          NOT NULL AUTO_INCREMENT,
    dept_name VARCHAR(150) NOT NULL,
    dept_code VARCHAR(20)           DEFAULT NULL,
    PRIMARY KEY (dept_id)
) ENGINE=InnoDB;

-- =============================================================
-- 3. STUDENTS
-- =============================================================
CREATE TABLE students (
    student_id  INT         NOT NULL AUTO_INCREMENT,
    user_id     INT         NOT NULL,
    roll_number VARCHAR(30) NOT NULL UNIQUE,
    dept_id     INT                  DEFAULT NULL,
    semester    TINYINT     NOT NULL DEFAULT 1,
    section     VARCHAR(10)          DEFAULT NULL,
    batch       VARCHAR(20)          DEFAULT NULL,   -- e.g. 2024-2028
    dob         DATE                 DEFAULT NULL,
    gender      ENUM('Male','Female','Other') NOT NULL DEFAULT 'Male',
    PRIMARY KEY (student_id),
    CONSTRAINT fk_stu_user FOREIGN KEY (user_id)  REFERENCES users(user_id),
    CONSTRAINT fk_stu_dept FOREIGN KEY (dept_id)  REFERENCES departments(dept_id)
) ENGINE=InnoDB;

-- =============================================================
-- 4. FACULTY
-- =============================================================
CREATE TABLE faculty (
    faculty_id    INT         NOT NULL AUTO_INCREMENT,
    user_id       INT         NOT NULL,
    employee_id   VARCHAR(30)          DEFAULT NULL UNIQUE,
    dept_id       INT                  DEFAULT NULL,
    designation   VARCHAR(100)         DEFAULT NULL,
    qualification VARCHAR(150)         DEFAULT NULL,
    joining_date  DATE                 DEFAULT NULL,
    PRIMARY KEY (faculty_id),
    CONSTRAINT fk_fac_user FOREIGN KEY (user_id)  REFERENCES users(user_id),
    CONSTRAINT fk_fac_dept FOREIGN KEY (dept_id)  REFERENCES departments(dept_id)
) ENGINE=InnoDB;

-- =============================================================
-- 5. COURSES
-- =============================================================
CREATE TABLE courses (
    course_id   INT         NOT NULL AUTO_INCREMENT,
    course_name VARCHAR(150) NOT NULL,
    course_code VARCHAR(20)  NOT NULL UNIQUE,
    credits     TINYINT     NOT NULL DEFAULT 3,
    dept_id     INT                  DEFAULT NULL,
    semester    TINYINT     NOT NULL DEFAULT 1,
    course_type ENUM('Core','Elective','Lab','Project') NOT NULL DEFAULT 'Core',
    PRIMARY KEY (course_id),
    CONSTRAINT fk_crs_dept FOREIGN KEY (dept_id) REFERENCES departments(dept_id)
) ENGINE=InnoDB;

-- =============================================================
-- 6. TIMETABLE
-- =============================================================
CREATE TABLE timetable (
    slot_id      INT         NOT NULL AUTO_INCREMENT,
    course_id    INT         NOT NULL,
    faculty_id   INT         NOT NULL,
    dept_id      INT                  DEFAULT NULL,
    semester     TINYINT     NOT NULL DEFAULT 1,
    section      VARCHAR(10)          DEFAULT NULL,
    day_of_week  ENUM('Monday','Tuesday','Wednesday','Thursday','Friday','Saturday') NOT NULL,
    start_time   TIME        NOT NULL,
    end_time     TIME        NOT NULL,
    room         VARCHAR(50)          DEFAULT NULL,
    PRIMARY KEY (slot_id),
    CONSTRAINT fk_tt_course  FOREIGN KEY (course_id)  REFERENCES courses(course_id),
    CONSTRAINT fk_tt_faculty FOREIGN KEY (faculty_id) REFERENCES faculty(faculty_id),
    CONSTRAINT fk_tt_dept    FOREIGN KEY (dept_id)    REFERENCES departments(dept_id)
) ENGINE=InnoDB;

-- =============================================================
-- 7. ATTENDANCE
-- =============================================================
CREATE TABLE attendance (
    att_id     INT  NOT NULL AUTO_INCREMENT,
    student_id INT  NOT NULL,
    course_id  INT  NOT NULL,
    att_date   DATE NOT NULL,
    status     ENUM('Present','Absent','Late','Excused') NOT NULL DEFAULT 'Present',
    PRIMARY KEY (att_id),
    UNIQUE KEY uq_att (student_id, course_id, att_date),
    CONSTRAINT fk_att_stu FOREIGN KEY (student_id) REFERENCES students(student_id),
    CONSTRAINT fk_att_crs FOREIGN KEY (course_id)  REFERENCES courses(course_id)
) ENGINE=InnoDB;

-- =============================================================
-- 8. RESULTS
-- =============================================================
CREATE TABLE results (
    result_id   INT            NOT NULL AUTO_INCREMENT,
    student_id  INT            NOT NULL,
    course_id   INT            NOT NULL,
    semester    TINYINT        NOT NULL,
    internal    DECIMAL(5,2)   NOT NULL DEFAULT 0.00,
    external    DECIMAL(5,2)   NOT NULL DEFAULT 0.00,
    total       DECIMAL(5,2)   GENERATED ALWAYS AS (internal + external) STORED,
    grade       VARCHAR(5)              DEFAULT NULL,
    grade_point DECIMAL(4,2)            DEFAULT 0.00,
    exam_year   YEAR                    DEFAULT NULL,
    PRIMARY KEY (result_id),
    UNIQUE KEY uq_result (student_id, course_id, semester, exam_year),
    CONSTRAINT fk_res_stu FOREIGN KEY (student_id) REFERENCES students(student_id),
    CONSTRAINT fk_res_crs FOREIGN KEY (course_id)  REFERENCES courses(course_id)
) ENGINE=InnoDB;

-- =============================================================
-- 9. ASSIGNMENTS
-- =============================================================
CREATE TABLE assignments (
    assign_id   INT          NOT NULL AUTO_INCREMENT,
    course_id   INT          NOT NULL,
    faculty_id  INT          NOT NULL,
    title       VARCHAR(200) NOT NULL,
    description TEXT                  DEFAULT NULL,
    due_date    DATE                  DEFAULT NULL,
    max_marks   INT          NOT NULL DEFAULT 100,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (assign_id),
    CONSTRAINT fk_asgn_crs FOREIGN KEY (course_id)  REFERENCES courses(course_id),
    CONSTRAINT fk_asgn_fac FOREIGN KEY (faculty_id) REFERENCES faculty(faculty_id)
) ENGINE=InnoDB;

-- =============================================================
-- 10. SUBMISSIONS
-- =============================================================
CREATE TABLE submissions (
    sub_id       INT          NOT NULL AUTO_INCREMENT,
    assign_id    INT          NOT NULL,
    student_id   INT          NOT NULL,
    remarks      TEXT                  DEFAULT NULL,
    status       ENUM('Submitted','Graded','Late','Missing') NOT NULL DEFAULT 'Submitted',
    marks        DECIMAL(5,2)          DEFAULT NULL,
    submitted_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sub_id),
    UNIQUE KEY uq_sub (assign_id, student_id),
    CONSTRAINT fk_sub_asgn FOREIGN KEY (assign_id)  REFERENCES assignments(assign_id),
    CONSTRAINT fk_sub_stu  FOREIGN KEY (student_id) REFERENCES students(student_id)
) ENGINE=InnoDB;

-- =============================================================
-- 11. FEES
-- =============================================================
CREATE TABLE fees (
    fee_id     INT            NOT NULL AUTO_INCREMENT,
    student_id INT            NOT NULL,
    semester   TINYINT        NOT NULL,
    fee_type   VARCHAR(80)    NOT NULL,              -- Tuition, Hostel, Library …
    amount     DECIMAL(10,2)  NOT NULL,
    due_date   DATE                    DEFAULT NULL,
    paid_date  DATE                    DEFAULT NULL,
    status     ENUM('Pending','Paid','Overdue','Waived') NOT NULL DEFAULT 'Pending',
    PRIMARY KEY (fee_id),
    CONSTRAINT fk_fee_stu FOREIGN KEY (student_id) REFERENCES students(student_id)
) ENGINE=InnoDB;

-- =============================================================
-- 12. HOSTEL ROOMS
-- =============================================================
CREATE TABLE hostel_rooms (
    room_id     INT           NOT NULL AUTO_INCREMENT,
    room_number VARCHAR(20)   NOT NULL,
    block       VARCHAR(50)            DEFAULT NULL,
    room_type   ENUM('Single','Double','Triple','Dormitory') NOT NULL DEFAULT 'Double',
    floor       TINYINT       NOT NULL DEFAULT 0,
    capacity    TINYINT       NOT NULL DEFAULT 2,
    occupied    TINYINT       NOT NULL DEFAULT 0,
    monthly_fee DECIMAL(8,2)  NOT NULL DEFAULT 0.00,
    PRIMARY KEY (room_id),
    UNIQUE KEY uq_room (block, room_number)
) ENGINE=InnoDB;

-- =============================================================
-- 13. HOSTEL ALLOTMENTS
-- =============================================================
CREATE TABLE hostel_allotments (
    allot_id   INT  NOT NULL AUTO_INCREMENT,
    student_id INT  NOT NULL,
    room_id    INT  NOT NULL,
    allot_date DATE NOT NULL,
    vacate_date DATE         DEFAULT NULL,
    status     ENUM('Active','Vacated') NOT NULL DEFAULT 'Active',
    PRIMARY KEY (allot_id),
    CONSTRAINT fk_ha_stu  FOREIGN KEY (student_id) REFERENCES students(student_id),
    CONSTRAINT fk_ha_room FOREIGN KEY (room_id)    REFERENCES hostel_rooms(room_id)
) ENGINE=InnoDB;

-- =============================================================
-- 14. LIBRARY BOOKS
-- =============================================================
CREATE TABLE library_books (
    book_id       INT          NOT NULL AUTO_INCREMENT,
    title         VARCHAR(255) NOT NULL,
    author        VARCHAR(200)          DEFAULT NULL,
    isbn          VARCHAR(20)           DEFAULT NULL UNIQUE,
    category      VARCHAR(100)          DEFAULT NULL,
    total_copies  INT          NOT NULL DEFAULT 1,
    available     INT          NOT NULL DEFAULT 1,
    rack_no       VARCHAR(20)           DEFAULT NULL,
    added_on      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (book_id)
) ENGINE=InnoDB;

-- =============================================================
-- 15. LIBRARY ISSUES
-- =============================================================
CREATE TABLE library_issues (
    issue_id    INT            NOT NULL AUTO_INCREMENT,
    book_id     INT            NOT NULL,
    user_id     INT            NOT NULL,
    issue_date  DATE           NOT NULL,
    due_date    DATE           NOT NULL,
    return_date DATE                    DEFAULT NULL,
    status      ENUM('Issued','Returned','Overdue') NOT NULL DEFAULT 'Issued',
    fine        DECIMAL(8,2)   NOT NULL DEFAULT 0.00,
    PRIMARY KEY (issue_id),
    CONSTRAINT fk_li_book FOREIGN KEY (book_id) REFERENCES library_books(book_id),
    CONSTRAINT fk_li_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB;

-- =============================================================
-- 16. NOTICES
-- =============================================================
CREATE TABLE notices (
    notice_id   INT          NOT NULL AUTO_INCREMENT,
    title       VARCHAR(200) NOT NULL,
    content     TEXT                  DEFAULT NULL,
    target_role ENUM('all','admin','faculty','student') NOT NULL DEFAULT 'all',
    posted_by   INT          NOT NULL,
    is_urgent   TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notice_id),
    CONSTRAINT fk_ntc_user FOREIGN KEY (posted_by) REFERENCES users(user_id)
) ENGINE=InnoDB;


-- =============================================================
--  SEED DATA
-- =============================================================

-- ── Departments ───────────────────────────────────────────────
INSERT INTO departments (dept_name, dept_code) VALUES
('Computer Science',          'CS'),
('Information Technology',    'IT'),
('Electrical Engineering',    'EE'),
('Civil Engineering',         'CE'),
('Business Administration',   'BA'),
('Medicine',                  'MED'),
('Law',                       'LAW'),
('Arabic & Islamic Studies',  'AIS');

-- ── Users (admin + sample faculty + sample students) ──────────
-- Passwords are stored as plain text here (same as original code).
-- Replace with BCrypt hashes before going to production.

INSERT INTO users (uid, password, role, full_name, email, phone) VALUES
-- Admin
('ADMIN-001',    'admin123',    'admin',   'System Administrator',       'admin@kfu.td',          '+235 60 00 00 01'),
-- Faculty
('FAC-2024-01',  'faculty123',  'faculty', 'Dr. Ibrahim Al-Hassan',      'ibrahim@kfu.td',         '+235 60 00 00 11'),
('FAC-2024-02',  'faculty123',  'faculty', 'Prof. Fatima Oumar',         'fatima@kfu.td',          '+235 60 00 00 12'),
('FAC-2024-03',  'faculty123',  'faculty', 'Dr. Moussa Mahamat',         'moussa@kfu.td',          '+235 60 00 00 13'),
('FAC-2024-04',  'faculty123',  'faculty', 'Dr. Amina Youssouf',         'amina@kfu.td',           '+235 60 00 00 14'),
-- Students
('KFU-2024-001', 'student123',  'student', 'Abdelkader Idriss',          'abdelkader@student.kfu.td', '+235 66 00 01 01'),
('KFU-2024-002', 'student123',  'student', 'Mariam Saleh',               'mariam@student.kfu.td',     '+235 66 00 01 02'),
('KFU-2024-003', 'student123',  'student', 'Youssouf Abdallah',          'youssouf@student.kfu.td',   '+235 66 00 01 03'),
('KFU-2024-004', 'student123',  'student', 'Hawa Mahamat',               'hawa@student.kfu.td',       '+235 66 00 01 04'),
('KFU-2024-005', 'student123',  'student', 'Oumar Brahim',               'oumar@student.kfu.td',      '+235 66 00 01 05'),
('KFU-2024-006', 'student123',  'student', 'Aisha Nour',                 'aisha@student.kfu.td',      '+235 66 00 01 06');

-- ── Faculty records ───────────────────────────────────────────
INSERT INTO faculty (user_id, employee_id, dept_id, designation, qualification, joining_date) VALUES
(2, 'EMP-001', 1, 'Associate Professor', 'PhD Computer Science',      '2018-09-01'),
(3, 'EMP-002', 1, 'Professor',           'PhD Software Engineering',  '2015-01-15'),
(4, 'EMP-003', 3, 'Assistant Professor', 'PhD Electrical Engineering','2020-08-20'),
(5, 'EMP-004', 5, 'Lecturer',            'MBA Finance',               '2022-02-10');

-- ── Student records ───────────────────────────────────────────
INSERT INTO students (user_id, roll_number, dept_id, semester, section, batch, dob, gender) VALUES
(6,  'CS-2024-001', 1, 3, 'A', '2022-2026', '2003-05-12', 'Male'),
(7,  'CS-2024-002', 1, 3, 'A', '2022-2026', '2003-08-22', 'Female'),
(8,  'IT-2024-001', 2, 1, 'B', '2024-2028', '2004-03-30', 'Male'),
(9,  'IT-2024-002', 2, 1, 'B', '2024-2028', '2004-11-05', 'Female'),
(10, 'EE-2024-001', 3, 5, 'A', '2020-2024', '2001-07-19', 'Male'),
(11, 'BA-2024-001', 5, 2, 'A', '2023-2027', '2003-12-01', 'Female');

-- ── Courses ───────────────────────────────────────────────────
INSERT INTO courses (course_name, course_code, credits, dept_id, semester, course_type) VALUES
('Introduction to Programming',       'CS101', 3, 1, 1, 'Core'),
('Data Structures & Algorithms',      'CS201', 3, 1, 3, 'Core'),
('Database Management Systems',       'CS301', 3, 1, 3, 'Core'),
('Operating Systems',                 'CS302', 3, 1, 3, 'Core'),
('Computer Networks',                 'CS401', 3, 1, 5, 'Core'),
('Software Engineering',              'CS402', 3, 1, 5, 'Core'),
('Web Technologies',                  'IT201', 3, 2, 3, 'Core'),
('Network Administration',            'IT301', 3, 2, 5, 'Core'),
('Circuit Theory',                    'EE101', 3, 3, 1, 'Core'),
('Digital Electronics',               'EE201', 3, 3, 3, 'Core'),
('Principles of Management',          'BA101', 3, 5, 1, 'Core'),
('Financial Accounting',              'BA201', 3, 5, 3, 'Core'),
('Islamic Studies',                   'GE001', 2, 8, 1, 'Core'),
('French Language',                   'GE002', 2, NULL,1, 'Core'),
('Mathematics I',                     'MA101', 3, NULL,1, 'Core');

-- ── Timetable ─────────────────────────────────────────────────
INSERT INTO timetable (course_id, faculty_id, dept_id, semester, section, day_of_week, start_time, end_time, room) VALUES
(2, 1, 1, 3, 'A', 'Monday',    '08:00:00', '09:30:00', 'CS-101'),
(3, 2, 1, 3, 'A', 'Monday',    '10:00:00', '11:30:00', 'CS-102'),
(4, 1, 1, 3, 'A', 'Tuesday',   '08:00:00', '09:30:00', 'CS-101'),
(7, 2, 2, 3, 'B', 'Tuesday',   '10:00:00', '11:30:00', 'IT-201'),
(2, 1, 1, 3, 'A', 'Wednesday', '08:00:00', '09:30:00', 'CS-101'),
(3, 2, 1, 3, 'A', 'Wednesday', '10:00:00', '11:30:00', 'CS-103'),
(12,4, 5, 3, 'A', 'Thursday',  '08:00:00', '09:30:00', 'BA-201'),
(4, 1, 1, 3, 'A', 'Thursday',  '10:00:00', '11:30:00', 'CS-101'),
(2, 1, 1, 3, 'A', 'Friday',    '08:00:00', '09:30:00', 'CS-101');

-- ── Attendance (sample) ───────────────────────────────────────
INSERT INTO attendance (student_id, course_id, att_date, status) VALUES
(1, 2, CURDATE() - INTERVAL 6 DAY, 'Present'),
(1, 2, CURDATE() - INTERVAL 5 DAY, 'Present'),
(1, 2, CURDATE() - INTERVAL 4 DAY, 'Absent'),
(1, 2, CURDATE() - INTERVAL 3 DAY, 'Present'),
(1, 3, CURDATE() - INTERVAL 6 DAY, 'Present'),
(1, 3, CURDATE() - INTERVAL 5 DAY, 'Late'),
(2, 2, CURDATE() - INTERVAL 6 DAY, 'Present'),
(2, 2, CURDATE() - INTERVAL 5 DAY, 'Present'),
(2, 3, CURDATE() - INTERVAL 6 DAY, 'Present'),
(3, 7, CURDATE() - INTERVAL 4 DAY, 'Present'),
(3, 7, CURDATE() - INTERVAL 3 DAY, 'Absent');

-- ── Results (sample) ─────────────────────────────────────────
INSERT INTO results (student_id, course_id, semester, internal, external, grade, grade_point, exam_year) VALUES
(1, 1, 1, 28.00, 55.00, 'B+', 7.00, YEAR(CURDATE()) - 2),
(1, 15,1, 30.00, 60.00, 'A',  8.00, YEAR(CURDATE()) - 2),
(1, 2, 3, 35.00, 58.00, 'A',  8.00, YEAR(CURDATE()) - 1),
(1, 3, 3, 32.00, 62.00, 'A+', 9.00, YEAR(CURDATE()) - 1),
(2, 1, 1, 38.00, 56.00, 'A',  8.00, YEAR(CURDATE()) - 2),
(2, 2, 3, 40.00, 55.00, 'A',  8.00, YEAR(CURDATE()) - 1),
(5, 5, 5, 36.00, 58.00, 'A',  8.00, YEAR(CURDATE()));

-- ── Assignments ───────────────────────────────────────────────
INSERT INTO assignments (course_id, faculty_id, title, description, due_date, max_marks) VALUES
(2, 1, 'Linked List Implementation',  'Implement singly and doubly linked lists in C++.',          CURDATE() + INTERVAL 7  DAY, 20),
(3, 2, 'ER Diagram – University DB',  'Draw a complete ER diagram for a university database.',    CURDATE() + INTERVAL 10 DAY, 25),
(7, 2, 'Responsive Portfolio Page',   'Build a responsive HTML/CSS portfolio page.',               CURDATE() + INTERVAL 5  DAY, 30),
(12,4, 'Balance Sheet Analysis',      'Analyse the provided company balance sheet for FY 2023.',  CURDATE() + INTERVAL 14 DAY, 20);

-- ── Submissions (sample) ─────────────────────────────────────
INSERT INTO submissions (assign_id, student_id, remarks, status, marks) VALUES
(1, 1, 'Implemented both variants with test cases.', 'Graded', 18.50),
(1, 2, 'Good work, minor memory leak.',              'Graded', 16.00),
(2, 1, 'Submitted on time.',                         'Submitted', NULL),
(3, 3, 'Used Bootstrap for responsiveness.',         'Submitted', NULL);

-- ── Fees ──────────────────────────────────────────────────────
INSERT INTO fees (student_id, semester, fee_type, amount, due_date, status) VALUES
(1, 3, 'Tuition Fee',  150000.00, CURDATE() - INTERVAL 30 DAY, 'Paid'),
(1, 3, 'Library Fee',    5000.00, CURDATE() - INTERVAL 30 DAY, 'Paid'),
(2, 3, 'Tuition Fee',  150000.00, CURDATE() - INTERVAL 30 DAY, 'Paid'),
(2, 3, 'Hostel Fee',    60000.00, CURDATE() + INTERVAL 5  DAY, 'Pending'),
(3, 1, 'Tuition Fee',  150000.00, CURDATE() - INTERVAL 10 DAY, 'Pending'),
(4, 1, 'Tuition Fee',  150000.00, CURDATE() + INTERVAL 15 DAY, 'Pending'),
(5, 5, 'Tuition Fee',  150000.00, CURDATE() - INTERVAL 60 DAY, 'Overdue'),
(6, 2, 'Tuition Fee',  150000.00, CURDATE() + INTERVAL 20 DAY, 'Pending');

-- ── Hostel rooms ──────────────────────────────────────────────
INSERT INTO hostel_rooms (room_number, block, room_type, floor, capacity, occupied, monthly_fee) VALUES
('A-101', 'Block A', 'Single',   1, 1, 0, 30000.00),
('A-102', 'Block A', 'Double',   1, 2, 1, 20000.00),
('A-103', 'Block A', 'Double',   1, 2, 2, 20000.00),
('A-201', 'Block A', 'Single',   2, 1, 1, 30000.00),
('B-101', 'Block B', 'Triple',   1, 3, 0, 15000.00),
('B-102', 'Block B', 'Dormitory',1, 6, 3, 10000.00),
('B-201', 'Block B', 'Double',   2, 2, 0, 20000.00),
('C-101', 'Block C', 'Single',   1, 1, 0, 30000.00);

-- ── Hostel allotments ─────────────────────────────────────────
INSERT INTO hostel_allotments (student_id, room_id, allot_date, status) VALUES
(2, 2, CURDATE() - INTERVAL 90 DAY, 'Active'),
(5, 6, CURDATE() - INTERVAL 60 DAY, 'Active'),
(6, 6, CURDATE() - INTERVAL 60 DAY, 'Active');

-- ── Library books ─────────────────────────────────────────────
INSERT INTO library_books (title, author, isbn, category, total_copies, available, rack_no) VALUES
('Introduction to Algorithms',                   'Cormen, Leiserson, Rivest', '978-0262033848', 'Computer Science',  5, 4, 'R-CS-01'),
('Database System Concepts',                     'Silberschatz, Korth',       '978-0073523323', 'Computer Science',  4, 3, 'R-CS-02'),
('Computer Networks',                            'Andrew S. Tanenbaum',       '978-0132126953', 'Networking',        3, 3, 'R-NET-01'),
('Operating System Concepts',                    'Silberschatz, Galvin',      '978-1118063330', 'Computer Science',  4, 2, 'R-CS-03'),
('Clean Code',                                   'Robert C. Martin',          '978-0132350884', 'Software Eng.',     3, 3, 'R-SE-01'),
('Principles of Management',                     'Robbins & Coulter',         '978-0133910698', 'Business',          5, 5, 'R-BA-01'),
('Financial Accounting',                         'Weygandt, Kimmel',          '978-1119300717', 'Accounting',        4, 4, 'R-BA-02'),
('Engineering Mathematics',                      'K.A. Stroud',               '978-1137031204', 'Mathematics',       6, 6, 'R-MA-01'),
('Digital Design',                               'M. Morris Mano',            '978-0132774208', 'Electronics',       3, 3, 'R-EE-01'),
('Holy Quran – Tafsir Ibn Kathir (Arabic)',       'Ibn Kathir',                NULL,             'Islamic Studies',   10,10, 'R-IS-01'),
('French Grammar in Practice',                   'Anne Akyuz',                '978-2011554383', 'Language',          4, 4, 'R-LG-01'),
('The Art of War',                               'Sun Tzu',                   '978-1599869773', 'General',           2, 2, 'R-GN-01');

-- ── Library issues (sample) ───────────────────────────────────
INSERT INTO library_issues (book_id, user_id, issue_date, due_date, return_date, status, fine) VALUES
(1, 6, CURDATE() - INTERVAL 10 DAY, CURDATE() + INTERVAL 4  DAY, NULL,        'Issued',   0.00),
(2, 7, CURDATE() - INTERVAL 20 DAY, CURDATE() - INTERVAL 6  DAY, NULL,        'Overdue', 600.00),
(4, 8, CURDATE() - INTERVAL 5  DAY, CURDATE() + INTERVAL 9  DAY, NULL,        'Issued',   0.00),
(3, 6, CURDATE() - INTERVAL 30 DAY, CURDATE() - INTERVAL 16 DAY, CURDATE() - INTERVAL 17 DAY, 'Returned', 0.00);

-- Update available copies for issued books
UPDATE library_books SET available = available - 1 WHERE book_id IN (1, 2, 4);

-- ── Notices ───────────────────────────────────────────────────
INSERT INTO notices (title, content, target_role, posted_by, is_urgent) VALUES
('Welcome to King Faisal University – Chad',
 'The Academic Information System v2.0 is now live. All students and faculty can log in using their assigned credentials. For support contact IT at admin@kfu.td.',
 'all', 1, 0),

('Mid-Term Examination Schedule – Semester 3',
 'Mid-term examinations for Semester 3 will begin on the 15th of next month. Students are advised to check the timetable and prepare accordingly. No re-scheduling will be allowed.',
 'student', 1, 1),

('Faculty Meeting – Curriculum Review',
 'All faculty members are requested to attend the curriculum review meeting on Friday at 10:00 AM in the Main Conference Hall. Attendance is mandatory.',
 'faculty', 1, 1),

('Library Hours Extended',
 'The university library will remain open from 7:00 AM to 10:00 PM effective immediately. Students are encouraged to make use of the extended hours during examination preparation.',
 'all', 1, 0),

('Fee Payment Deadline Reminder',
 'All pending fee payments for Semester 3 must be cleared by the end of this month. Late payments will attract a penalty. Visit the Finance Office or pay through the portal.',
 'student', 1, 1),

('New Books Added to Library',
 'The library has received a new batch of books covering Computer Science, Business Administration, and Islamic Studies. Please visit the library or check the portal for the updated catalogue.',
 'all', 1, 0);

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================
-- Done. Connect using:
--   Host:     localhost
--   Port:     3306
--   Database: kfudb
--   User:     root
--   Password: Tchad235@
-- =============================================================
